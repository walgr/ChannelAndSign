#include "aes.h"
#include <assert.h>
#include <stdlib.h>
#include <string.h>
#include <stdio.h>

int main(void) {
    char plain[512];
    for (int i = 0; i < 512; ++i) plain[i] = (char)(i % 251);
    int encryptedLength = 0, decryptedLength = -1;
    char *encrypted = tiny_aes_encrypt_cbc(plain, sizeof(plain), &encryptedLength);
    assert(encrypted && encryptedLength == 528);
    char original[528];
    memcpy(original, encrypted, sizeof(original));
    assert(tiny_aes_decrypt_cbc(encrypted, encryptedLength, &decryptedLength));
    assert(decryptedLength == 512 && memcmp(encrypted, plain, 512) == 0);
    // Tampering with CBC's preceding block changes exactly one padding byte.
    memcpy(encrypted, original, 528);
    encrypted[496] ^= 1;
    assert(!tiny_aes_decrypt_cbc(encrypted, 528, &decryptedLength));
    assert(decryptedLength == 0);
    assert(!tiny_aes_decrypt_cbc(NULL, 528, &decryptedLength));
    assert(!tiny_aes_decrypt_cbc(encrypted, 527, &decryptedLength));
    free(encrypted);
    puts("Native AES payload tests passed");
    return 0;
}
