#include "sys.h"
#include "delay.h"
#include "usart.h"
#include "wifi.h"
#include "led.h"
#include "jdq.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>



void init_system()
{       
  delay_init();	    	 //��ʱ������ʼ��
		  
  NVIC_Configuration(); 	      //����NVIC�жϷ���2:2λ��ռ���ȼ���2λ��Ӧ���ȼ�
  
  init_leds();        //��ʼ��led
  init_jdqs();        //��ʼ���̵���

  uart1_init(9600);	 //����1��ʼ��Ϊ9600 ������
  //uart2_init(115200);	 //����2��ʼ��Ϊ115200 WIFIģ����
  Uart2Init();		   //����2��ʼ��Ϊ115200 WIFIģ����

  uart1_send("System Running...\r\n\0",19);
}

void loop()
{
  //��ʾϵͳ������Ϣ
  if (Serial_available()) {    				  //WIFI��Ϣ ���� 
	wifi_process_msg(); 
  }
 
}

int main(void)
{		
  init_system(); 

  while (1) {
    loop();
  }
}

