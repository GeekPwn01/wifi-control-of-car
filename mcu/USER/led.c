#include <string.h>

#include <stdio.h>
#include <stdlib.h>

#include "usart.h"
#include "delay.h"

#define RUN_STATUS_LED GPIOB,GPIO_Pin_8


void set_gps_status_led(unsigned char status)
{
  if (status > 0)
    GPIO_ResetBits(RUN_STATUS_LED);
  else
    GPIO_SetBits(RUN_STATUS_LED);
}

void init_leds(void)
{
  GPIO_InitTypeDef GPIO_InitStructure;
  
  //ÉèÖÃ IO PB8 ÎªÍÆÍì
  RCC_APB2PeriphClockCmd(RCC_APB2Periph_GPIOB,ENABLE);
  GPIO_InitStructure.GPIO_Pin = GPIO_Pin_8;
  GPIO_InitStructure.GPIO_Mode = GPIO_Mode_Out_PP;
  GPIO_InitStructure.GPIO_Speed = GPIO_Speed_50MHz;
  GPIO_Init(GPIOB,&GPIO_InitStructure);
  
  set_gps_status_led(0);  //µÆÃð
}
