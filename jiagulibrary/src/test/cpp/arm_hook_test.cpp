#include "dobby.h"
#include <cassert>
#include <cstdio>

extern "C" int arm_basic(int), thumb_basic(int), arm_literal(int), thumb_literal(int), thumb2_unaligned_literal(int);
__asm__(
    ".text\n.arm\n.balign 16\n.global arm_basic\n.type arm_basic,%function\n"
    "arm_basic:\n push {r4,lr}\n add r0,r0,#3\n nop\n pop {r4,pc}\n"
    ".size arm_basic,.-arm_basic\n"
    ".thumb\n.balign 16\n.global thumb_basic\n.type thumb_basic,%function\n.thumb_func\n"
    "thumb_basic:\n push {r4,lr}\n adds r0,#5\n nop\n nop\n pop {r4,pc}\n"
    ".size thumb_basic,.-thumb_basic\n"
    ".arm\n.balign 16\n.global arm_literal\n.type arm_literal,%function\n"
    "arm_literal:\n ldr r1,.Larm_value\n add r0,r0,r1\n bx lr\n nop\n.Larm_value: .word 9\n"
    ".size arm_literal,.-arm_literal\n"
    ".thumb\n.balign 16\n.global thumb_literal\n.type thumb_literal,%function\n.thumb_func\n"
    "thumb_literal:\n ldr r1,.Lthumb_value\n adds r0,r0,r1\n nop\n nop\n bx lr\n"
    ".balign 4\n.space 8\n.Lthumb_value: .word 11\n"
    ".size thumb_literal,.-thumb_literal\n"
    ".thumb\n.balign 16\n.global thumb2_unaligned_literal\n.type thumb2_unaligned_literal,%function\n.thumb_func\n"
    "thumb2_unaligned_literal:\n push {r4,lr}\n mov r4,r0\n nop\n ldr.w r1,.Lthumb2_value\n"
    "adds r0,r4,r1\n pop {r4,pc}\n.balign 4\n.space 8\n.Lthumb2_value: .word 13\n"
    ".size thumb2_unaligned_literal,.-thumb2_unaligned_literal\n.thumb\n");
using Fn = int (*)(int);
static Fn original[5];
static int replacement0(int x) { return original[0](x) * 2; }
static int replacement1(int x) { return original[1](x) * 2; }
static int replacement2(int x) { return original[2](x) * 2; }
static int replacement3(int x) { return original[3](x) * 2; }
static int replacement4(int x) { return original[4](x) * 2; }
int main() {
    Fn functions[] = {arm_basic, thumb_basic, arm_literal, thumb_literal, thumb2_unaligned_literal};
    Fn replacements[] = {replacement0, replacement1, replacement2, replacement3, replacement4};
    const int increments[] = {3, 5, 9, 11, 13};
    for (int i = 0; i < 5; ++i) {
        assert(functions[i](7) == 7 + increments[i]);
        assert(DobbyHook(reinterpret_cast<void *>(functions[i]),
            reinterpret_cast<dobby_dummy_func_t>(replacements[i]),
            reinterpret_cast<dobby_dummy_func_t *>(&original[i])) == 0);
        assert(original[i] && original[i](7) == 7 + increments[i]);
        assert(functions[i](7) == (7 + increments[i]) * 2);
    }
    puts("ARM_HOOK_PASSED: ARM/Thumb entry, original trampoline, PC-relative literal loads");
}
