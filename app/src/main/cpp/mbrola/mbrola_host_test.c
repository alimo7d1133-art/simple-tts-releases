#include <stdio.h>
#include <stdlib.h>
#define DLL_EXPORT
#include "incdll.h"
#include "parser.h"
#include "onechannel.h"
int main(int argc,char **argv){
 if(argc!=2){fprintf(stderr,"usage: mbrola_host_test PATH_TO_EN1\n");return 2;}
 if(init_MBR(argv[1])!=0){fprintf(stderr,"init_MBR failed\n");return 1;}
 setNoError_MBR(1); if(write_MBR("_ 40\nb 80\na 120\nt 80\n_ 40\n")<=0||flush_MBR()<=0){fprintf(stderr,"write failed\n");return 1;}
 short buf[4096]; int total=0,n; while((n=read_MBR(buf,4096))>0)total+=n; printf("sample_rate=%d samples=%d\n",getFreq_MBR(),total); close_MBR(); return total>0?0:1;
}
