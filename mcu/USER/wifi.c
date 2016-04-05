//使用串口2 :定时器2， P10 ->RXD2, P11 -> TXD2


#include <string.h>

#include <stdio.h>
#include <stdlib.h>
#include "led.h"
#include "usart.h"
#include "delay.h"
#include "jdq.h"

#define RS232_QUEUE_CNT 12

static volatile unsigned char  uart2buf[128];
static volatile unsigned char uart2Index = 0;
static volatile unsigned char busy = 0;



//用循环队列保存数据
typedef struct rs232_queue {
  unsigned char Tail;
  unsigned char Head;
  char nData[RS232_QUEUE_CNT][128];
  unsigned char len[RS232_QUEUE_CNT];
}RS232_QUEUE;

RS232_QUEUE g_queue;

static unsigned char IsEmpty()
{
  if (g_queue.Tail == g_queue.Head)
    return 1;
  else
    return 0;
}


static void SetEmpty()
{  
  g_queue.Tail = 0;
  g_queue.Head = 0; 
}

static unsigned char IsFull()
{
  if (((g_queue.Tail + 1) % RS232_QUEUE_CNT) == g_queue.Head)
    return 1;
  else
    return 0;
}

char debug_msg[128];
static unsigned char EnQueue(volatile char *value,unsigned char len)
{ 
  
  memcpy((char*)&g_queue.nData[g_queue.Tail][0],(char*)value,len);
  g_queue.nData[g_queue.Tail][len] = '\0';
  g_queue.len[g_queue.Tail] = len;

  //sprintf(debug_msg,"   EnQueue:%d,%s\r\n\0",g_queue.Tail,&g_queue.nData[g_queue.Tail][0]);
  //debug(debug_msg,strlen(debug_msg));
  //sprintf(debug_msg,"   EnQueue: len=%d\r\n\0",(int)len);
  //uart1_send(debug_msg,strlen(debug_msg));

  /*
  sprintf(debug_msg,"%x %x %x %x %x %x %x %x %x\r\n\0",
	    g_queue.nData[g_queue.Tail][0],g_queue.nData[g_queue.Tail][1],g_queue.nData[g_queue.Tail][2],
		g_queue.nData[g_queue.Tail][3],g_queue.nData[g_queue.Tail][4],
		g_queue.nData[g_queue.Tail][5],g_queue.nData[g_queue.Tail][6],g_queue.nData[g_queue.Tail][7],
		g_queue.nData[g_queue.Tail][8]);
  uart1_send(debug_msg,strlen(debug_msg));
  */

  g_queue.Tail = (g_queue.Tail + 1) % RS232_QUEUE_CNT; 
  return (g_queue.Tail - g_queue.Head) % RS232_QUEUE_CNT;
}

static unsigned char DeQueue(char *buf,unsigned char *len)
{
  unsigned char value = 1;  
  //memcpy(buf,(char*)&g_queue.nData[g_queue.Head][0],strlen((char*)&g_queue.nData[g_queue.Head])); 
  memcpy(buf,(char*)&g_queue.nData[g_queue.Head][0],g_queue.len[g_queue.Head]); 
  *len = g_queue.len[g_queue.Head];
  
  //sprintf(debug_msg,"   DeQueue:%d,%s\r\n\0",g_queue.Head,&g_queue.nData[g_queue.Head][0]);
  //debug(debug_msg,strlen(debug_msg));
  /*
  sprintf(debug_msg,"DeQueue: len=%d,%x %x %x %x %x %x %x %x %x\r\n\0",strlen((char*)&g_queue.nData[g_queue.Head][0]),
	    g_queue.nData[g_queue.Head][0],g_queue.nData[g_queue.Head][1],g_queue.nData[g_queue.Head][2],
		g_queue.nData[g_queue.Head][3],g_queue.nData[g_queue.Head][4],
		g_queue.nData[g_queue.Head][5],g_queue.nData[g_queue.Head][6],g_queue.nData[g_queue.Head][7],
		g_queue.nData[g_queue.Head][8]);
  uart1_send(debug_msg,strlen(debug_msg));
  */	  
   
  g_queue.Head = (g_queue.Head + 1) % RS232_QUEUE_CNT;  
  return value;
}

unsigned char Serial_available()
{
  if (IsEmpty())
    return 0;
  else 
    return 1;
}

unsigned char Serial_read(char *msg,unsigned char *len)
{
  return DeQueue(msg,len);  
}

////////////////////////////////////////////////////////////

void Uart2Init(void)		//9600bps@11.0592MHz
{
  SetEmpty();
  uart2Index = 0;
  uart2_init(115200);    
}

void USART2_IRQHandler(void)                	//串口3中断服务程序
{
  unsigned char Res;
  char msg[128];
  //溢出-如果发生溢出需要先读SR,再读DR寄存器则可清除不断入中断的问题[牛人说要这样]
  if(USART_GetFlagStatus(USART2,USART_FLAG_ORE)==SET) {
     USART_ClearFlag(USART2,USART_FLAG_ORE);
     USART_ReceiveData(USART2);     
	 //sprintf(debug_msg," UART2 IRQ ORE\r\n\0");
	 //debug(debug_msg,strlen(debug_msg));
  }

  if(USART_GetITStatus(USART2, USART_IT_RXNE) == SET) { //接收中断(接收到的数据必须是0x0d 0x0a结尾)	 
    //USART_ClearITPendingBit(USART2, USART_IT_RXNE);
    Res = USART_ReceiveData(USART2);		//(USART1->DR);	//读取接收到的数据
	USART_ClearFlag(USART2,USART_FLAG_RXNE);		


    //sprintf(msg,"\r\nuart2 recv:%d\r\n\0",Res);
	//uart1_send("\r\nuart2 recv\r\n\0",15);
	//uart1_send(msg,strlen(msg));
	uart2buf[uart2Index] = Res;
	if (uart2Index == 0 && uart2buf[uart2Index] != 0x81)
	  return;
	uart2Index++;
	if (uart2Index == 8) {
	  sprintf(msg,"\r\n%x %x %x %x %x %x %x %x\r\n\0",
	    uart2buf[0],uart2buf[1],uart2buf[2],uart2buf[3],uart2buf[4],
		uart2buf[5],uart2buf[6],uart2buf[7]);
	  uart1_send(msg,strlen(msg));
	  if (!IsFull()) 
	    EnQueue((char*)uart2buf,8);	 		
	  uart2Index = 0;
	}
  } 
  
} 



void send_uart2_msg(char *msg,unsigned char cnt)
{

  uart2_send(msg,cnt);
}


void wifi_process_msg()
{
  #define UART2_BUF_LENGTH 64
  char g_uart2_msg[UART2_BUF_LENGTH];
  int len,i = 0;
  unsigned char msglen = 0;
  char msg[16];

  memset((char*)g_uart2_msg,0,UART2_BUF_LENGTH);
  Serial_read((char*)g_uart2_msg,&msglen);   
  
  len = msglen;
  sprintf(msg,"len=%d\r\n\0",len);
  uart1_send(msg,strlen(msg));

  for (i = 0; i < len; i++) {
    sprintf(msg,"%d \0",g_uart2_msg[i]);
    uart1_send(msg,strlen(msg)); 
  }

  if (g_uart2_msg[0] == 0x81) {
    if (g_uart2_msg[1] == 0x1) { //开灯
	  set_gps_status_led(1);
	  uart1_send("\r\nopen light\r\n\0",15);
	}
  
    if (g_uart2_msg[1] == 0x2) { //关灯
	  set_gps_status_led(0);
	  uart1_send("\r\nclose light\r\n\0",16);
	}

	if (g_uart2_msg[1] == 0x3) { //开继电器
	  set_jdq1_status(1);
	  uart1_send("\r\nopen JDQ\r\n\0",15);
	}
  
    if (g_uart2_msg[1] == 0x4) { //关继电器
	  set_jdq1_status(0);
	  uart1_send("\r\nclose JDQ\r\n\0",16);
	}
  }
  return;
}


