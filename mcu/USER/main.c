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
  delay_init();	    	 //延时函数初始化
		  
  NVIC_Configuration(); 	      //设置NVIC中断分组2:2位抢占优先级，2位响应优先级
  
  init_leds();        //初始化led
  init_jdqs();        //初始化继电器

  uart1_init(9600);	 //串口1初始化为9600 调试用
  //uart2_init(115200);	 //串口2初始化为115200 WIFI模块用
  Uart2Init();		   //串口2初始化为115200 WIFI模块用

  uart1_send("System Running...\r\n\0",19);
}

void loop()
{
  //显示系统参数信息
  if (Serial_available()) {    				  //WIFI消息 处理 
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

