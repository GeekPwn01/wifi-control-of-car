#include <string.h>

#include <stdio.h>
#include <stdlib.h>

#include "usart.h"
#include "delay.h"

#define JDQ1 GPIOB,GPIO_Pin_14
#define JDQ2 GPIOB,GPIO_Pin_15


void set_jdq1_status(unsigned char status)
{
  if (status > 0)
    GPIO_ResetBits(JDQ1);
  else
    GPIO_SetBits(JDQ1);
}

void set_jdq2_status(unsigned char status)
{
  if (status > 0)
    GPIO_ResetBits(JDQ2);
  else
    GPIO_SetBits(JDQ2);
}


void init_jdqs(void)
{
  GPIO_InitTypeDef GPIO_InitStructure;
  
  //设置 IO PB8 为推挽
  RCC_APB2PeriphClockCmd(RCC_APB2Periph_GPIOB,ENABLE);
  GPIO_InitStructure.GPIO_Pin = GPIO_Pin_14 | GPIO_Pin_15;
  GPIO_InitStructure.GPIO_Mode = GPIO_Mode_Out_PP;
  GPIO_InitStructure.GPIO_Speed = GPIO_Speed_50MHz;
  GPIO_Init(GPIOB,&GPIO_InitStructure);
  
  set_jdq1_status(0);  //关继电器
  set_jdq2_status(0);  //关继电器
}
