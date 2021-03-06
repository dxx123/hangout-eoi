package com.ctrip.ops.sysdev;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.DailyRollingFileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import com.ctrip.ops.sysdev.configs.HangoutConfig;
import com.ctrip.ops.sysdev.inputs.BaseInput;

public class Main {
	private static final Logger logger = Logger.getLogger(Main.class.getName());

	public class Option {
		String flag, opt;

		public Option(String flag, String opt) {
			this.flag = flag;
			this.opt = opt;
		}
	}

	/**
	 * parse the input command arguments
	 * 
	 * @param args
	 * @return
	 * @throws ParseException
	 */
	private static CommandLine parseArg(String[] args) throws ParseException {
		Options options = new Options();
		options.addOption("h", false, "usage help");
		options.addOption("help", false, "usage help");
		options.addOption("f", true, "configuration file");
		options.addOption("l", true, "log file");
		options.addOption("w", true, "filter worker number");
		options.addOption("v", false, "print info log");
		options.addOption("vv", false, "print debug log");
		options.addOption("vvvv", false, "print trace log");

		CommandLineParser paraer = new BasicParser();
		CommandLine cmdLine = paraer.parse(options, args);

		if (cmdLine.hasOption("help") || cmdLine.hasOption("h")) {
			usage();
			System.exit(-1);
		}

		// TODO need process invalid arguments
		if (!cmdLine.hasOption("f")) {
			throw new IllegalArgumentException("需要 -f 参数指定配置文件");
		}
		return cmdLine;
	}

	/**
	 * print help information
	 */
	private static void usage() {
		StringBuilder helpInfo = new StringBuilder();
		helpInfo.append("-h").append("\t\t\thelp command").append("\n").append("-help").append("\t\t\thelp command")
				.append("\n").append("-f").append("\t\t\trequired config, indicate config file").append("\n")
				.append("-l").append("\t\t\tlog file that store the output").append("\n").append("-w")
				.append("\t\t\tfilter worker numbers").append("\n").append("-v").append("\t\t\tprint info log")
				.append("\n").append("-vv").append("\t\t\tprint debug log").append("\n").append("-vvvv")
				.append("\t\t\tprint trace log").append("\n");

		System.out.println(helpInfo.toString());
	}

	/**
	 * Setup logger according arguments
	 * 
	 * @param cmdLine
	 */
	private static void setupLogger(CommandLine cmdLine) {
		if (cmdLine.hasOption("l")) {
			DailyRollingFileAppender fa = new DailyRollingFileAppender();
			fa.setName("FileLogger");
			fa.setFile(cmdLine.getOptionValue("l"));
			fa.setLayout(new PatternLayout("%d %p %C %t %m%n"));
			if (cmdLine.hasOption("vvvv")) {
				fa.setThreshold(Level.TRACE);
				Logger.getRootLogger().setLevel(Level.TRACE);
			} else if (cmdLine.hasOption("vv")) {
				fa.setThreshold(Level.DEBUG);
			} else if (cmdLine.hasOption("v")) {
				fa.setThreshold(Level.INFO);
			} else {
				fa.setThreshold(Level.WARN);
			}
			fa.setAppend(true);
			fa.activateOptions();
			Logger.getRootLogger().addAppender(fa);
		} else {
			ConsoleAppender console = new ConsoleAppender();
			String PATTERN = "%d %p %C %t %m%n";
			console.setLayout(new PatternLayout(PATTERN));
			if (cmdLine.hasOption("vvvv")) {
				console.setThreshold(Level.TRACE);
				Logger.getRootLogger().setLevel(Level.TRACE);
			} else if (cmdLine.hasOption("vv")) {
				console.setThreshold(Level.DEBUG);
			} else if (cmdLine.hasOption("v")) {
				console.setThreshold(Level.INFO);
			} else {
				console.setThreshold(Level.WARN);
			}
			console.activateOptions();
			Logger.getRootLogger().addAppender(console);
		}
	}

	// 程序入口
	public static void main(String[] args) throws Exception {
		CommandLine cmdLine = parseArg(args);
		setupLogger(cmdLine);

		String fileName = cmdLine.getOptionValue("f");
		// 解析配置文件--第三方snakeryaml库解析yaml
		Map configs = HangoutConfig.parse(fileName);

		// 获取用户配置的所有input
		ArrayList<Map> inputList = (ArrayList<Map>) configs.get("inputs");
		// 获取用户配置的所有filter
		ArrayList<Map> filterList = (ArrayList<Map>) configs.get("filters");
		// 获取用户配置的所有output
		ArrayList<Map> outputList = (ArrayList<Map>) configs.get("outputs");

		// 遍历所有配置的input
		for (Map input : inputList) {
			Iterator<Entry<String, Map>> inputIT = input.entrySet().iterator();

			while (inputIT.hasNext()) {
				Map.Entry<String, Map> inputEntry = inputIT.next();
				// 获取配置的input类型名（类名）
				String inputType = inputEntry.getKey();
				// 获取input对应的配置项，Map类型
				Map inputConfig = inputEntry.getValue();
				// 通过反射实例化相应的input类，并调用业务逻辑方法
				Class<?> inputClass = Class.forName("com.ctrip.ops.sysdev.inputs." + inputType);
				Constructor<?> ctor = inputClass.getConstructor(Map.class, ArrayList.class, ArrayList.class);
				BaseInput inputInstance = (BaseInput) ctor.newInstance(inputConfig, filterList, outputList);
				inputInstance.emit();
			}
		}
	}
}
