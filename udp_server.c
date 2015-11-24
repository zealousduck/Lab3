/*
** listener.c -- a datagram sockets "server" demo
*/

#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <errno.h>
#include <string.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <netdb.h>

#define MYPORT "10015"	//GID of our group and the server
#define PACKET_SIZE 5

#define MAXBUFLEN 100

int main(int argc, char *argv[])
{
	int sockfd;
	struct addrinfo hints, *servinfo, *p;
	int rv;
	int numbytes;
	struct sockaddr_in their_addr;
	char buf[MAXBUFLEN];
	socklen_t addr_len;
	char s[INET6_ADDRSTRLEN];
	int waiting = 0; //no one's connected - no waiting
	const char magicNumber = 0xA5;

	if (argc < 2) {
		printf("Usage: Server Serverport#\n");
		exit(1);
	}

	memset(&hints, 0, sizeof hints);
	hints.ai_family = AF_UNSPEC; // set to AF_INET to force IPv4
	hints.ai_socktype = SOCK_DGRAM;
	hints.ai_flags = AI_PASSIVE; // use my IP

	if ((rv = getaddrinfo(NULL, argv[1], &hints, &servinfo)) != 0) {
		fprintf(stderr, "getaddrinfo: %s\n", gai_strerror(rv));
		return 1;
	}

    for(p = servinfo; p != NULL; p = p->ai_next) {
        if ((sockfd = socket(p->ai_family, p->ai_socktype,
                             p->ai_protocol)) == -1) {
            perror("server: socket");
            continue;
        }

        if (bind(sockfd, p->ai_addr, p->ai_addrlen) == -1) {
            close(sockfd);
            perror("listener: bind");
            continue;
        }
        break;
    }

        if (p == NULL) {
            fprintf(stderr, "listener: failed to bind socket\n");
            return 2;
        }


	freeaddrinfo(servinfo);

	printf("listener: waiting to recvfrom...\n");
    
    char errorCode;
	while (1) {
		addr_len = sizeof their_addr;
        memset(buf, 0, MAXBUFLEN); // clear buffer beforehand
		if ((numbytes = recvfrom(sockfd, buf, 5, 0,
			(struct sockaddr *)&their_addr, &addr_len)) == -1) {
			perror("recvfrom");
			exit(1);
		}

		for (int i = 0; i < PACKET_SIZE; i++) {
			printf("0x%x ", buf[i] & 0xff);
		}
	    printf("\n");

		int ipAddr = (int)(their_addr.sin_addr.s_addr);
		uint16_t portAddr = ntohs(their_addr.sin_port);
	
		unsigned char ipAddress[4];
		ipAddress[0] = ipAddr & 0xff;
		ipAddress[1] = (ipAddr >> 8) & 0xff;
		ipAddress[2] = (ipAddr >> 16) & 0xff;
		ipAddress[3] = (ipAddr >> 24) & 0xff;

		printf("%d.%d.%d.%d\n", ipAddress[0], ipAddress[1], ipAddress[2], ipAddress[3]);

    	int theirGID = buf[4];
    	int theirPort = (buf[2] << 8) + buf[3];
	
		int magic = 0xa5;
   
   		errorCode= 0x00;
   		
    	if ((buf[0] & 0xff) != magic && (buf[1] & 0xff) != magic) {
    	   errorCode += 1;
    	}

    	//printf("%u", errorCode);
    	if (numbytes != 5) {
    	    errorCode += 2;
    	}
    	//printf("%u", errorCode);

    	if (theirPort < (10010 + 5 * theirGID) || theirPort > (10010 + 5 * theirGID + 4)) {
    	    errorCode += 4;
    	}
    	printf("Error code: %u\n", errorCode);
    	//if error, send error packet
		if (errorCode != 0x00) {
            printf("Sending error packet\n");
			char errorPacket[5];
			memset(errorPacket,0,5);
			errorPacket[0] = (unsigned char)0xA5;
			errorPacket[1] = (unsigned char)0xA5;
			errorPacket[2] = (unsigned char)1;
        	errorPacket[3] = (unsigned char)0x00;
        	errorPacket[4] = (unsigned char)errorCode;

			if ((numbytes = sendto(sockfd, (char*)&errorPacket, 5, 0,
				(struct sockaddr *)&their_addr, addr_len)) == -1) {
				perror("sendto");
				exit(1);
			}
		}

		//if no client is waiting to talk
		else if (waiting == 0) {
            printf("There's no client waiting..\n");
			waiting = 1;
			char waitPacket[5];
			memset(waitPacket,0,5);
			waitPacket[0] = (unsigned char)0xA5;
			waitPacket[1] = (unsigned char)0xA5;
			waitPacket[2] = (unsigned char)1;
            waitPacket[3] = buf[2];
            waitPacket[4] = buf[3];

			if ((numbytes = sendto(sockfd, (char*)&waitPacket, 5, 0,
				(struct sockaddr *)&their_addr, addr_len)) == -1) {
				perror("sendto");
				exit(1);
			}

			//waiting = 0; // why was this here?
		}

		else { // (waiting == 1) {
            printf("There's a client waiting to talk!\n");
			char connectPacket[9];
			memset(connectPacket,0,9);
			connectPacket[0] = (unsigned char)0xA5;
			connectPacket[1] = (unsigned char)0xA5;
			connectPacket[2] = ipAddress[0];
        	connectPacket[3] = ipAddress[1];
        	connectPacket[4] = ipAddress[2];
        	connectPacket[5] = ipAddress[3];
            connectPacket[6] = buf[2];
            connectPacket[7] = buf[3];
			connectPacket[8] = (unsigned char)1;

			if ((numbytes = sendto(sockfd, (char*)&connectPacket, 9, 0,
				(struct sockaddr *)&their_addr, addr_len)) == -1) {
				perror("sendto");
				exit(1);
			}
            waiting = 0;
		}
	}	

	close(sockfd);

	return 0;
}
