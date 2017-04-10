package cc.cxsj.nju.gobang.info;

import cc.cxsj.nju.gobang.config.ServerProperties;

public class ContestResult {
	
	private static final int ROUNDS = Integer.valueOf(ServerProperties.instance().getProperty("contest.rounds"));
	private static final int ERRORS = Integer.valueOf(ServerProperties.instance().getProperty("step.error.number"));
	
	public int id;
	public Player[] players;
	public int winner = -1;
	public int[] winRound;
	public int[] stepsNum;
	public int[][] scores;
	public int[][] errors;
	
	public ContestResult() {
		this.players = new Player[2];
		this.winRound = new int[2];
		this.stepsNum = new int[ROUNDS];
		this.scores = new int[2][ROUNDS];
		this.errors = new int[2][ROUNDS];
	}
	
	public ContestResult(int id, Player user1, Player user2) {
		this.id = id;
		this.players = new Player[2];
		this.players[0] = user1;
		this.players[1] = user2;
		this.winRound = new int[2];
		this.stepsNum = new int[ROUNDS];
		this.scores = new int[2][ROUNDS];
		this.errors = new int[2][ROUNDS];
	}
	
	public void evaluate() {
		if (winner != -1) {
			return;
		}
		for (int round = 0; round < ROUNDS; round++) {
			if (errors[0][round] > ERRORS) {
				winRound[1] += 3;
				continue;
			}
			if (errors[1][round] > ERRORS) {
				winRound[0] += 3;
				continue;
			}
			if (scores[0][round] > scores[1][round]) {
				winRound[0] += 3;
			} else if (scores[0][round] < scores[1][round]) {
				winRound[1] += 3;
			} 
			else {
				winRound[0]++;
				winRound[1]++;
			}
		}
		if (winRound[0] > winRound[1]) {
			winner = 0;
		} else if (winRound[0] < winRound[1]) {
			winner = 1;
		} else {
			winner = 2;
		}
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("");
		sb.append("CONTEST/TEST/DEBUG ");
		sb.append(this.id);
		sb.append("\nPLAYER ");
		sb.append(this.players[0].getId());
		sb.append(" VS ");
		sb.append(this.players[1].getId());
		sb.append("\nWINNER ");
		sb.append(this.winner);
		sb.append("\nFINAL_SCORE ");
		sb.append(this.winRound[0]);
		sb.append(":");
		sb.append(this.winRound[1]);
		sb.append("\nSCORE_OF_ROUND ");
		for (int r = 0; r < ROUNDS; r++) {
			sb.append(this.scores[0][r]);
			sb.append(":");
			sb.append(this.scores[1][r]);
			sb.append(" ");
		}
		sb.append("\nSTEP_NUMBER_OF_ROUND ");
		for (int r = 0; r < ROUNDS; r++) {
			sb.append(this.stepsNum[r]);
			sb.append(" ");
		}
		sb.append("\nERROR_NUMBER_OF_ROUND ");
		for (int r = 0; r < ROUNDS; r++) {
			sb.append(this.errors[0][r]);
			sb.append(":");
			sb.append(this.errors[1][r]);
			sb.append(" ");
		}
		return sb.toString();
	}
	
}
