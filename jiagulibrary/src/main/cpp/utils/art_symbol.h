#pragma once
// Resolve an exact ART function name, including stripped mini-debug symbols.
void *resolve_art_symbol(void *handle, const char *path, const char *name);
