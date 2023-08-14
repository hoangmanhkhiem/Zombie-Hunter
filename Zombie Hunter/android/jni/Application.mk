APP_OPTIM := release
APP_STL := gnustl_static
APP_CPPFLAGS := -frtti -fexceptions -Wformat=0 -std=c++11 -DGOOGLE_PLAY_STORE -DBB20 -fsigned-char
APP_LDFLAGS := -latomic
APP_ABI := armeabi armeabi-v7a x86
APP_PLATFORM := android-10