#include "Define.h"
#include "Square.h"
#include "ClientSocket.h"
#include "UNChineseChess.h"
#include <windows.h>
#include <iostream>
#include <stdlib.h>
#include <stdio.h>

#define random(x) (rand()%x)
#define ROWS 15
#define COLS 15
#define ROUNDS 3

Square board[ROWS][COLS];
int ownColor = -1, oppositeColor = -1;

void authorize(const char *id, const char *pass) {
	connectServer();
    std::cout << "Authorize " << id << std::endl;
	char msgBuf[BUFSIZE];
	memset(msgBuf, 0, BUFSIZE);
	msgBuf[0] = 'A';
	memcpy(&msgBuf[1], id, 9);
    memcpy(&msgBuf[10], pass, 6);
    int rtn = sendMsg(msgBuf);
	if (rtn != 0) printf("Authorized Failed");
}

void gameStart() {
	authorize(ID, PASSWORD);
    std::cout << "Game Start" << std::endl;
	for (int round = 0; round < ROUNDS; round++) {
		roundStart(round);
		oneRound();
		roundOver(round);
	}
	gameOver();
	close();
}

void gameOver() {
	std::cout << "Game Over" << std::endl;
}

void roundStart(int round) {
	std::cout << "Round " << round << " Start" << std::endl;
	for (int r = 0; r < ROWS; r++) {
		for (int c = 0; c < COLS; c++) {
			board[r][c].reset();
		}
	}
    int rtn = recvMsg();
	if (rtn != 0) return;
	printf("Round start received msg %s\n", recvBuf);
	switch (recvBuf[1]) {
        case 'B':
            ownColor = 0;
            oppositeColor = 1;
			rtn = sendMsg("BB");
			if (rtn != 0) return;
            break;
        case 'W':
            ownColor = 1;
            oppositeColor = 0;
			rtn = sendMsg("BW");
			if (rtn != 0) return;
            break;
    }
}

void oneRound() {
	switch (ownColor) {
        case 0:
            while (true) {
                step();                     // take action, send message
                if (observe() >= 1) break;  // receive RET Code
                // saveChessBoard();
                if (observe() >= 1) break;  // receive others move
                // saveChessBoard();
            }
			printf("One Round End\n");
            break;
        case 1:
            while (true) {
                if (observe() >= 1) break;  // receive others move
                // saveChessBoard();
                step();                     // take action, send message
                if (observe() >= 1) break;  // receive RET code
                // saveChessBoard();
            }
			printf("One Round End\n");
            break;
        default:
            break;
	}
}

void roundOver(int round) {
	std::cout << "Round " << round << " Over" << std::endl;
    for (int r = 0; r < ROWS; r++) {
        for (int c = 0; c < COLS; c++) {
            board[r][c].reset();
        }
    }
    ownColor = oppositeColor = -1;
}

int observe() {
    int rtn = 0;
    int recvrtn = recvMsg();
	if (recvrtn != 0) return 1;
	printf("receive msg %s\n", recvBuf);
    switch (recvBuf[0]) {
        case 'R':   // return messages
        {
            switch (recvBuf[1]) {
                case '0':    // valid step
                    switch (recvBuf[2]) {
                        case 'P':   // update chessboard
                        {
                            int desRow = (recvBuf[3] - '0') * 10 + recvBuf[4] - '0';
                            int desCol = (recvBuf[5] - '0') * 10 + recvBuf[6] - '0';
							board[desRow][desCol].color = recvBuf[7] - '0';
							board[desRow][desCol].empty = false;
                            break;
                        }
                        case 'N':   // R0N: enemy wrong step
                        {
                            break;
                        }
                    }
                    break;
				case 1:
					std::cout << "Error -1: Msg format error";
					rtn = -1;
					break;
				case 2:
					std::cout << "Error -2: Coordinate error";
					rtn = -2;
					break;
				case 3:
					std::cout << "Error -3: Piece color error";
					rtn = -3;
					break;
				case 4:
					std::cout << "Error -4: Invalid step";
					rtn = -4;
					break;
                default:
					std::cout << "Error -5: Other error";
                    rtn = -5;
                    break;
            }
            break;
        }
        case 'E':
        {
			switch (recvBuf[1]) {
                case '0':
                    // game over
                    rtn = 2;
                    break;
                case '1':
                    // round over
                    rtn = 1;
                default:
                    break;
            }
            break;
        }
    }
    return rtn;
}

void putDown(int row, int col) {
	char msg[6];
	msg[0] = 'S';
	msg[1] = 'P';
	msg[2] = '0' + row / 10;
	msg[3] = '0' + row % 10;
	msg[4] = '0' + col / 10;
	msg[5] = '0' + col % 10;
	board[row][col].color = ownColor;
	board[row][col].empty = false;
	sendMsg(msg);
	printf("put down (%c%c, %c%c)\n", msg[2], msg[3], msg[4], msg[5]);
}

void noStep() {
	sendMsg("SN");
	printf("send msg %s\n", "SN");
}

void saveChessBoard() {
	for (int r = 0; r < ROWS; r++) {
		for (int c = 0; c < COLS; c++) {
			if (board[r][c].color == -1)
				printf("E ");
			else
			    printf("%d ", board[r][c].color);
		}
		printf("\n");
	}
	puts("");
}

void step() {

	int r = -1, c = -1;
	while (!(r >= 0 && r < ROWS && c >= 0 && c < COLS && board[r][c].empty)) {
		r = random(15);
		c = random(15);
		// System.out.println("Rand " + r + " " + c);
	}
	putDown(r, c);
}

