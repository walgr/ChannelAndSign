#pragma once

// Install once during shell initialization, before business classes are loaded.
// Unsupported prologues fail without changing the target. Successful hooks live
// for the lifetime of the process; concurrent installation/unhook is unsupported.
bool install_x86_hook(void *target, void *replacement, void **original);
