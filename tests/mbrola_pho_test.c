/* Old TTS native regression harness, AGPL-3.0-or-later. */
#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>
#include <string.h>
#include "incdll.h"
#include "parser.h"
#include "onechannel.h"
static void u16(FILE*f,unsigned n){fputc(n&255,f);fputc((n>>8)&255,f);}
static void u32(FILE*f,unsigned n){u16(f,n&65535);u16(f,n>>16);}
int main(int argc,char**argv){
 if(argc<4){fprintf(stderr,"database commands.pho out.wav [tolerate]\n");return 2;}
 FILE *in=fopen(argv[2],"rb");if(!in)return 3;fseek(in,0,SEEK_END);long len=ftell(in);rewind(in);if(len<0||len>1000000)return 3;
 char *commands=calloc((size_t)len+1,1);if(!commands)return 3;fread(commands,1,(size_t)len,in);fclose(in);
 if(init_MBR(argv[1])!=0)return 4;setNoError_MBR(argc>4?atoi(argv[4]):1);
 if(write_MBR(commands)<=0||flush_MBR()<=0)return 5;free(commands);
 FILE *out=fopen(argv[3],"wb+");if(!out)return 6;
 fwrite("RIFF",1,4,out);u32(out,0);fwrite("WAVEfmt ",1,8,out);u32(out,16);u16(out,1);u16(out,1);int hz=getFreq_MBR();u32(out,hz);u32(out,hz*2);u16(out,2);u16(out,16);fwrite("data",1,4,out);u32(out,0);
 short pcm[4096];int total=0,n;long long energy=0;while((n=read_MBR(pcm,4096))>0){for(int i=0;i<n;i++){u16(out,(unsigned short)pcm[i]);energy+=abs(pcm[i]);}total+=n;}
 fseek(out,4,SEEK_SET);u32(out,total*2+36);fseek(out,40,SEEK_SET);u32(out,total*2);fclose(out);close_MBR();
 printf("database=%s Hz=%d samples=%d energy=%lld read=%d\n",argv[1],hz,total,energy,n);return n<0||total<1000||energy<10000?1:0;
}
