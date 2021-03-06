package cc.cxsj.nju.gobang.ui;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import cc.cxsj.nju.gobang.config.ServerProperties;

public class SwingConsole {
	
	public static void run(final JFrame f){
		int width = Integer.valueOf(ServerProperties.instance().getProperty("ui.width"));
		int height = Integer.valueOf(ServerProperties.instance().getProperty("ui.height"));
		SwingUtilities.invokeLater(new Runnable(){
			@Override
			public void run() {
				f.pack();
				f.setTitle("GobanG 1.0.1");
				f.setLocationRelativeTo(null);
				f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
				f.setSize(width, height);
				f.setVisible(true);
			}
		});
	}
}
