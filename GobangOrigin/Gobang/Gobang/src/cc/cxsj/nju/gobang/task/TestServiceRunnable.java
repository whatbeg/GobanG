package cc.cxsj.nju.gobang.task;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.SocketTimeoutException;
import java.util.ArrayList;

import cc.cxsj.nju.gobang.chess.ChessBoard;
import cc.cxsj.nju.gobang.config.ServerProperties;
import cc.cxsj.nju.gobang.info.ContestResults;
import cc.cxsj.nju.gobang.info.Player;
import org.apache.log4j.Logger;

import cc.cxsj.nju.gobang.ai.RobotAIModel;
import cc.cxsj.nju.gobang.info.ContestResult;
import cc.cxsj.nju.gobang.info.RobotPlayerAdapter;
import cc.cxsj.nju.gobang.ui.MainFrame;

public class TestServiceRunnable implements Runnable{

    private static final Logger LOG = Logger.getLogger(TestServiceRunnable.class);
    private static final int ROUNDS = Integer.valueOf(ServerProperties.instance().getProperty("contest.rounds"));
    private static final int STEPS = Integer.valueOf(ServerProperties.instance().getProperty("round.steps"));
    private static final int ERRORS = Integer.valueOf(ServerProperties.instance().getProperty("step.error.number"));
    private static final RobotAIModel ROBOT_MODEL = RobotAIModel.values()[Integer.valueOf(ServerProperties.instance().getProperty("robot.model"))];
    private static int ID = 0;

    private Player[] players;
    private String info;
    private ContestResult result;
    private ArrayList<ArrayList<String>> record;

    public TestServiceRunnable(Player user) {

        this.players = new Player[2];
        players[0] = user;
        players[1] = new RobotPlayerAdapter(ROBOT_MODEL.toString(), ROBOT_MODEL.toString(), ROBOT_MODEL);

        this.info = players[0].getId() + "vs" + players[1].getId() + "-test-" + ID;

        this.result = new ContestResult(ID, players[0], players[1]);

        this.record = new ArrayList<ArrayList<String>>();
        for (int r = 0; r < ROUNDS; r++) {
            this.record.add(new ArrayList<String>());
            record.get(r).add("TEST " + ID);
            record.get(r).add("INFO " + players[0].getId() + " VS " + players[1].getId());
        }

        ID++;
    }

    @Override
    public void run() {

        LOG.info(this.info + " begin!");
        MainFrame.instance().log(this.info + " begin!");

        byte[] recvBuffer = null;

        try {

            for (int round = 0; round < ROUNDS; round++) {

                record.get(round).add("ROUND_START " + round);
                LOG.info(this.info + " round " + round + " start");
                MainFrame.instance().log(this.info + " round " + round + " start");

                // assign color piece
                int black = round & 0x1, white = 1 - black;
                String synMsg = null;
                if (black == 0) {
                    synMsg = "BB";
                } else {
                    synMsg = "BW";
                }
                int num = 0;
                record.get(round).add("COLOR BLACK:P" + black + " WHITE:P" + white);

                // generate empty chess board
                ChessBoard board = new ChessBoard();
                board.generateEmptyChessBoard();
                // record.get(round).add("INITIAL CHESS BOARD\n" + board.toStringToRecord());

                try {

                    try {
                        System.out.println("players[black].send(BB)");
                        players[black].send("BB");
                        System.out.println("players[black].send(BB) end");
                    } catch (Exception e) {
                        LOG.error(e);
                        record.get(round).add("SEND_ERROR BLACK");
                        result.winner = white;
                        return;
                    }
                    try {
                        System.out.println("players[white].send(BW)");
                        players[white].send("BW");
                        System.out.println("players[white].send(BW) end");
                    } catch (Exception e) {
                        LOG.error(e);
                        record.get(round).add("SEND_ERROR WHITE");
                        result.winner = black;
                        return;
                    }

                    int synNum = 0;
                    try {
                        while (synNum < 5) {
                            recvBuffer = players[0].receive();
                            String syn = new String(recvBuffer);
                            if (syn.substring(0, 2).equals(synMsg)) {
                                break;
                            }
                            synNum++;
                        }
                        if (synNum >= 5) {
                            // syn fail too much time
                            result.errors[0][round]++;
                            LOG.error(this.info + " ROUND " + round + " Sync. Failed too much(5) times");
                            record.get(round).add("SYNTIME_EXCEED BLACK " + result.errors[0][round]);
                            result.winner = 1;
                            return;
                        }
                    } catch (SocketTimeoutException e) {
                        // step timeout
                        result.errors[0][round]++;
                        LOG.error(e);
                        LOG.error(this.info + " ROUND " + round + " TimeoutException when synchronize black round");
                        record.get(round).add("SYN_ERROR BLACK " + result.errors[0][round]);
                        result.winner = 1;
                        return;
                    } catch (Exception e) {
                        // other exception
                        LOG.error(e);
                        LOG.error(this.info + " ROUND " + round + " Unkown Exception when synchronize black round!");
                        record.get(round).add("UNKOWN_EXCEPTION BLACK");
                        result.winner = 1;
                        return;
                    }

                    // begin palying chess
                    for ( ; num < STEPS && board.isGeneratedWinnner() < 0; num++) {

                        // receive black player step
                        try {
                            // block...
                            recvBuffer = players[black].receive();
                        } catch (SocketTimeoutException e) {
                            // step timeout
                            result.errors[black][round]++;
                            LOG.error(e);
                            LOG.error(this.info + " ROUND " + round + " TimeoutException when receive BLACK step: " + result.errors[black][round] + " time!");
                            record.get(round).add("TIMEOUT BLACK " + result.errors[black][round]);
                            while (result.errors[black][round] <= ERRORS) {
                                try {
                                    recvBuffer = players[black].receive();
                                } catch (SocketTimeoutException ee) {
                                    result.errors[black][round]++;
                                    LOG.error(e);
                                    LOG.error(this.info + " ROUND " + round + " TimeoutException when receive BLACK step: " + result.errors[black][round] + " time!");
                                    record.get(round).add("TIMEOUT BLACK " + result.errors[black][round]);
                                    continue;
                                }
                                break;
                            }
                            if (result.errors[black][round] > ERRORS) {
                                record.get(round).add("ERROR_MAXTIME BLACK");
                                break;
                            }
                        } catch (Exception e) {
                            // other exception
                            LOG.error(e);
                            LOG.error(this.info + " ROUND " + round + " Unkown Exception when receive BLACK step!");
                            record.get(round).add("UNKOWN_EXCEPTION BLACK");
                            result.winner = white;
                            break;
                        }

                        // test and verify the black step
                        String blackStep = new String(recvBuffer);
                        String blackReturnCode = board.step(blackStep, 0);
                        if (blackReturnCode.charAt(1) == '0') {
                            // valid step
                            record.get(round).add("VALID_STEP BLACK " + blackStep.substring(0, 6));
                            record.get(round).add(board.toStringToDisplay());
                            try {
                                players[black].send(blackReturnCode);
                            } catch (Exception e) {
                                LOG.error(e);
                                record.get(round).add("SEND_ERROR BLACK");
                                result.winner = white;
                                return;
                            }
                            try {
                                players[white].send(blackReturnCode);
                            } catch (Exception e) {
                                LOG.error(e);
                                record.get(round).add("SEND_ERROR WHITE");
                                result.winner = black;
                                return;
                            }
                        } else {
                            // invalid step
                            result.errors[black][round]++;
                            record.get(round).add("ERROR_STEP BLACK " + result.errors[black][round] + " " + blackStep.substring(0, 6));
                            try {
                                players[black].send(blackReturnCode);
                            } catch (Exception e) {
                                LOG.error(e);
                                record.get(round).add("SEND_ERROR BLACK");
                                result.winner = white;
                                return;
                            }
                            try {
                                players[white].send("R0N");
                            } catch (Exception e) {
                                LOG.error(e);
                                record.get(round).add("SEND_ERROR WHITE");
                                result.winner = black;
                                return;
                            }
                            if (result.errors[black][round] > ERRORS) {
                                record.get(round).add("ERROR_MAXTIME BLACK");
                                break;
                            }
                        }

                        if (board.isGeneratedWinnner() >= 0) // a player won
                            break;

                        // receive black step
                        try {
                            // block...
                            recvBuffer = players[white].receive();
                        } catch (SocketTimeoutException e) {
                            // step timeout
                            result.errors[white][round]++;
                            LOG.error(e);
                            LOG.error(this.info + " ROUND " + round + " TimeoutException when receive WHITE step: "
                                    + result.errors[white][round] + " time!");
                            record.get(round).add("TIMEOUT WHITE " + result.errors[white][round]);
                            while (result.errors[white][round] <= ERRORS) {
                                try {
                                    recvBuffer = players[white].receive();
                                } catch (SocketTimeoutException ee) {
                                    result.errors[white][round]++;
                                    LOG.error(e);
                                    LOG.error(this.info + " ROUND " + round + " TimeoutException when receive WHITE step: "
                                            + result.errors[white][round] + " time!");
                                    record.get(round).add("TIMEOUT WHITE " + result.errors[white][round]);
                                    continue;
                                }
                                break;
                            }
                            if (result.errors[white][round] > ERRORS) {
                                record.get(round).add("ERROR_MAXTIME WHITE");
                                break;
                            }
                        } catch (Exception e) {
                            // other exception
                            LOG.error(e);
                            LOG.error(this.info + " ROUND " + round + " Unkown Exception when receive WHITE step!");
                            record.get(round).add("UNKOWN_EXCEPTION WHITE");
                            result.winner = black;
                            break;
                        }

                        // test and verify the black step
                        String whiteStep = new String(recvBuffer);
                        String whiteReturnCode = board.step(whiteStep, 1);
                        if (whiteReturnCode.charAt(1) == '0') {
                            // valid step
                            record.get(round).add("VALID_STEP WHITE " + whiteStep.substring(0, 6));
                            record.get(round).add(board.toStringToDisplay());
                            try {
                                players[white].send(whiteReturnCode);
                            } catch (Exception e) {
                                LOG.error(e);
                                record.get(round).add("SEND_ERROR WHITE");
                                result.winner = black;
                                return;
                            }
                            try {
                                players[black].send(whiteReturnCode);
                            } catch (Exception e) {
                                LOG.error(e);
                                record.get(round).add("SEND_ERROR BLACK");
                                result.winner = white;
                                return;
                            }
                        } else {
                            // invalid step
                            result.errors[white][round]++;
                            record.get(round).add("ERROR_STEP WHITE " + result.errors[white][round] + " " + whiteStep.substring(0, 6));
                            try {
                                players[white].send(whiteReturnCode);
                            } catch (Exception e) {
                                LOG.error(e);
                                record.get(round).add("SEND_ERROR WHITE");
                                result.winner = black;
                                return;
                            }
                            try {
                                players[black].send("R0N");
                            } catch (Exception e) {
                                LOG.error(e);
                                record.get(round).add("SEND_ERROR BLACK");
                                result.winner = white;
                                return;
                            }
                            if (result.errors[white][round] > ERRORS) {
                                record.get(round).add("ERROR_MAXTIME WHITE");
                                break;
                            }
                        }
                    }
                } catch (Exception e) {
                    // round end abnormally
                    e.printStackTrace();
                    LOG.error(e);
                    LOG.error(this.info + " ROUND " + round + " Unkown Exception in " + round + " CONTEST");
                    record.get(round).add("ROUND_ERROR " + round);
                } finally {
                    // notify players that this round is over and record this round result
                    try {
                        System.out.println("Send E1");
                        players[0].send("E1");
                        players[1].send("E1");
                    } catch (Exception e) {
                        LOG.error(e);
                    }
                    LOG.info(this.info + " round " + round + " end");
                    MainFrame.instance().log(this.info + " round " + round + " end");

                    // record result
                    result.stepsNum[round] = num;
                    result.scores[black][round] = board.isGeneratedWinnner() == 0 ? 1 : 0;
                    result.scores[white][round] = board.isGeneratedWinnner() == 1 ? 1 : 0;

                    record.get(round).add("ROUND_END " + round);
                }

            }

        } catch (Exception e) {
            LOG.error(e);
            LOG.error(this.info + " Unkown Exception");
        } finally {
            try {
                // notify players that contest is over and save contest result
                System.out.println("Send E0");
                players[0].send("E0");
                players[1].send("E0");
            } catch (Exception e) {
                LOG.error(e);
            }
            LOG.info(this.info + " game over");
            MainFrame.instance().log(this.info + " game over");

            // save result
            result.evaluate();  // evaluate who is winner
            ContestResults.addContestResult(result);  // ContestResults.put(id, result)
            saveResult();                             // save results to file, PrintWriter.println()
            LOG.info(this.info + " result save completed!");
            MainFrame.instance().log(this.info + " result save completed!");

            // store record into file, make one file for every round
            saveRecord();
            LOG.info(this.info + " record save completed!");
            MainFrame.instance().log(this.info + " record save completed!");

            // release players, game over
            players[0].clear();
            players[1].clear();
        }
    }

    private void saveRecord() {
        String RECORD_DIR = ServerProperties.instance().getProperty("current.record.dir");
        File dir = new File(System.getProperty("user.dir") + "/record");
        if (!dir.exists()) {
            dir.mkdir();
        }
        dir = new File(System.getProperty("user.dir") + "/record/test");
        if (!dir.exists()) {
            dir.mkdir();
        }
        dir = new File(System.getProperty("user.dir") + "/record/test/" + RECORD_DIR);
        if (!dir.exists()) {
            dir.mkdir();
        }
        for (int r = 0; r < ROUNDS; r++) {
            PrintWriter out = null;
            try {
                File file = new File(System.getProperty("user.dir") + "/record/test/" + RECORD_DIR + "/" + this.info + "-round-" + r + ".record");
                if (!file.exists()) {
                    file.createNewFile();
                } else {
                    file.delete();
                    file.createNewFile();
                }
                out = new PrintWriter(file);
                ArrayList<String> rec = record.get(r);
                for (int i = 0; i < rec.size(); i++) {
                    out.println(rec.get(i));
                }
                out.flush();
            } catch (FileNotFoundException e) {
                LOG.error(e);
            } catch (IOException e) {
                LOG.error(e);
            } finally {
                record.get(r).clear();
                out.close();
            }
        }
    }

    public void saveResult() {
        String RESULT_DIR = ServerProperties.instance().getProperty("current.result.dir");
        PrintWriter out = null;
        try {
            File dir = new File(System.getProperty("user.dir") + "/result");
            if (!dir.exists()) {
                dir.mkdir();
            }
            dir = new File(System.getProperty("user.dir") + "/result/test");
            if (!dir.exists()) {
                dir.mkdir();
            }
            dir = new File(System.getProperty("user.dir") + "/result/test/" + RESULT_DIR);
            if (!dir.exists()) {
                dir.mkdir();
            }
            File file = new File(System.getProperty("user.dir") + "/result/test/" + RESULT_DIR + "/" + this.info + ".result");
            if (!file.exists()) {
                file.createNewFile();
            } else {
                file.delete();
                file.createNewFile();
            }
            out = new PrintWriter(file);
            out.println(result);
            out.flush();
        } catch (FileNotFoundException e) {
            LOG.error(e);
        } catch (IOException e) {
            LOG.error(e);
        } finally {
            result = null;
            out.close();
        }
    }

}
