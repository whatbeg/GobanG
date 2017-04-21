package cc.cxsj.nju.gobang.ai;

import cc.cxsj.nju.gobang.Main;
import org.apache.log4j.Logger;

public class RobotAIFactory {
	
	private static final Logger LOG = Logger.getLogger(Main.class);
	
	public static RobotAI produceRobotAIof(RobotAIModel model) {
		switch (model) {
		case RobotO:
		case RobotI:
			LOG.info("Produce one Robot I");
			return new RobotI();
		case RobotII:
			LOG.info("Produce on Robot II");
			return new RobotII();
		case RobotIII:
		case RobotIV:
		default:
			LOG.info("Robot Factory can not produce this model Robot!");
			System.exit(0);
		}
		return null;
	}
}
