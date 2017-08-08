package main;

import java.util.Scanner;

public class Debugger {

	private static Debugger instance;
	public static Debugger getInstance() {
		if(instance == null)
			instance = new Debugger();
		return instance;
	}
	
	private Scanner sysinScanner;
	
	private Debugger() {
		sysinScanner = new Scanner(System.in);
	}
	
	public Scanner getSysinScanner() {
		return this.sysinScanner;
	}
}
