#ifndef __GSM_H
#define __GSM_H

void Uart2Init(void);
void wifi_init(void);
void wifi_process_msg(void);
unsigned char send_sms(void);
unsigned char Serial_available(void);
unsigned char Serial_read(char *msg);

#endif
