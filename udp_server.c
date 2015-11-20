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

	addr_len = sizeof their_addr;
	if ((numbytes = recvfrom(sockfd, buf, 5, 0,
		(struct sockaddr *)&their_addr, &addr_len)) == -1) {
		perror("recvfrom");
		exit(1);
	}

	int ipAddr = (int)(their_addr.sin_addr.s_addr);
	uint16_t portAddr = ntohs(their_addr.sin_port);

    int theirGID = buf[4];
    int theirPort = (buf[2] << 8) + buf[3];

	//check for errors
    uint8_t errorCode = 0x00;
    
    if (buf[0] != magicNumber && buf[1] != magicNumber) {
        errorCode += 1;
    }
    
    if (numbytes != 5) {
        errorCode += 2;
    }
    
    if (theirPort < (10010 + 5 * theirGID) || theirPort > (10010 + 5 * theirGID + 4)) {
        errorCode += 4;
    }
    
    //if error, send error packet
	if (errorCode != 0x00) {
		char errorPacket[5];
		memset(errorPacket,0,5);
		errorPacket[0] = (unsigned char)0xA5;
		errorPacket[1] = (unsigned char)0xA5;
		errorPacket[2] = (unsigned char)1;
        errorPacket[3] = (unsigned char)0x00;
        errorPacket[4] = (unsigned char)errorCode;

		if ((numbytes = sendto(sockfd, (char*)&errorPacket, 5, 0,
			p->ai_addr, p->ai_addrlen)) == -1) {
			perror("sendto");
			exit(1);
		}
	}

	if (waiting == 0) {
		waiting = 1;
		char noWaitPacket[5];
		memset(noWaitPacket,0,5);
		noWaitPacket[0] = (unsigned char)0xA5;
		noWaitPacket[1] = (unsigned char)0xA5;
		noWaitPacket[2] = (unsigned char)1;
		noWaitPacket[3] = (unsigned char)portAddr >> 8;
		noWaitPacket[4] = (unsigned char)portAddr >> 0;

		if ((numbytes = sendto(sockfd, (char*)&noWaitPacket, 5, 0,
			p->ai_addr, p->ai_addrlen)) == -1) {
			perror("sendto");
			exit(1);
		}

		waiting = 0;
	}

	if (waiting == 1) {
		char waitingPacket[9];
		memset(waitingPacket,0,9);
		waitingPacket[0] = (unsigned char)0xA5;
		waitingPacket[1] = (unsigned char)0xA5;
		waitingPacket[2] = (unsigned char)ipAddr >> 24;
        waitingPacket[3] = (unsigned char)ipAddr >> 16;
        waitingPacket[4] = (unsigned char)ipAddr >> 8;
        waitingPacket[5] = (unsigned char)ipAddr >> 0;
		waitingPacket[6] = (unsigned char)portAddr >> 8;
		waitingPacket[7] = (unsigned char)portAddr >> 0;
		waitingPacket[8] = (unsigned char)1;

		if ((numbytes = sendto(sockfd, (char*)&waitingPacket, 9, 0,
			p->ai_addr, p->ai_addrlen)) == -1) {
			perror("sendto");
			exit(1);
		}
	}

	close(sockfd);

	return 0;
}