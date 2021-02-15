package com.songoda.ultimatestacker.utils;

public class Paire<H, K> {
	
	private H firstElement;
	private K secondElement;
	
	public Paire(H firstElement, K secondElement) {
		this.firstElement = firstElement;
		this.secondElement = secondElement;
	}
	
	
	public H getFirstElement() {
		return firstElement;
	}
	
	
	public K getSecondElement() {
		return secondElement;
	}
	
	
	public void setFirstElement(H firstElement) {
		this.firstElement = firstElement;
	}
	
	public void setSecondElement(K secondElement) {
		this.secondElement = secondElement;
	}

}
