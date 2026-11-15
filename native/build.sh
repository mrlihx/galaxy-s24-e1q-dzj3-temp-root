#!/usr/bin/env bash
# Build the two exploit payloads used by the Galaxy S24U DZJ3 app.
# Requires: Android NDK r29 (or r26+), aarch64-linux-android34-clang.
#
#   export ANDROID_NDK_HOME=/path/to/android-ndk-r29
#   ./build.sh
#
# Outputs: libtimer_std.so, libtimer_lspd.so
set -euo pipefail
cd "$(dirname "$0")"

if [ -z "${ANDROID_NDK_HOME:-}" ]; then
  echo "ANDROID_NDK_HOME is required" >&2
  exit 1
fi

HOST_TAG="linux-x86_64"
case "$(uname -s)" in
  Darwin) HOST_TAG="darwin-x86_64" ;;
  MINGW*|MSYS*|CYGWIN*) HOST_TAG="windows-x86_64" ;;
esac

CC="$ANDROID_NDK_HOME/toolchains/llvm/prebuilt/$HOST_TAG/bin/aarch64-linux-android34-clang"
if [ ! -x "$CC" ]; then
  echo "compiler not found: $CC" >&2
  exit 1
fi

# NOTE: these payloads are exec'd directly by the app (ProcessBuilder on the
# file in nativeLibraryDir), so they must be built as PIE *executables*.
# Linking with -shared yields a DSO with e_entry=0 and no PT_INTERP: exec'ing
# it jumps to address 0 and dies with SIGSEGV before main() runs.
CFLAGS=(-O2 -fPIE -fno-stack-protector -Wno-unused-result -Wno-incompatible-pointer-types)
LDFLAGS=(-pie -s -Wl,--build-id=none)

"$CC" "${CFLAGS[@]}" "${LDFLAGS[@]}" -o libtimer_std.so timer-dzj3-apk.c
"$CC" "${CFLAGS[@]}" "${LDFLAGS[@]}" -o libtimer_lspd.so timer-dzj3-lsposed.c

echo "built:"
ls -l libtimer_std.so libtimer_lspd.so
