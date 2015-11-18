# # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # #
#									LAB 3 MAKEFILE							  #
# # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # #

all:
	make log
	make client
	make server
	
log:
	date
	ls -lt

client:
	javac Client.java

server:
	cc -o Server.out Server.c

clean:
	rm *.class
	rm *.out
