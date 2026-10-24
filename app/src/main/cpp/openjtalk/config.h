#ifndef OPENJTALK_MECAB_CONFIG_H
#define OPENJTALK_MECAB_CONFIG_H
#define HAVE_CTYPE_H 1
#define HAVE_DIRENT_H 1
#define HAVE_FCNTL_H 1
#define HAVE_GETENV 1
#define HAVE_GETPAGESIZE 1
#define HAVE_INTTYPES_H 1
#define HAVE_MEMORY_H 1
#define HAVE_STDINT_H 1
#define HAVE_STDLIB_H 1
#define HAVE_STRING_H 1
#define HAVE_SYS_MMAN_H 1
#define HAVE_SYS_STAT_H 1
#define HAVE_SYS_TYPES_H 1
#define HAVE_UNISTD_H 1
#define PACKAGE "mecab"
#define VERSION "0.996"
#define DIC_VERSION 102
#define MECAB_DEFAULT_CHARSET "UTF-8"
#define MECAB_DEFAULT_RC ""
#define SIZEOF_CHAR 1
#define SIZEOF_SHORT 2
#define SIZEOF_INT 4
#define SIZEOF_LONG_LONG 8
#if defined(__LP64__)
#define SIZEOF_LONG 8
#define SIZEOF_SIZE_T 8
#else
#define SIZEOF_LONG 4
#define SIZEOF_SIZE_T 4
#endif
#endif
