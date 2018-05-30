package com.insightfullogic.slab.performance;

import com.insightfullogic.slab.GameEvent;


public class GameEventPOJO implements GameEvent {
	
	private int id;
	private long strength;
	private int target;
	
	public GameEventPOJO() {}

	@Override
    public void move(int index) {
		throw new UnsupportedOperationException();
	}
	
	@Override
    public int getIndex() {
		throw new UnsupportedOperationException();
	}

	@Override
    public void close() {}

	@Override
    public int getId() {
		return id;
	}

	@Override
    public void setId(int id) {
		this.id = id;
	}

	@Override
    public long getStrength() {
		return strength;
	}

	@Override
    public void setStrength(long strength) {
		this.strength = strength;
	}

	@Override
    public int getTarget() {
		return target;
	}

	@Override
    public void setTarget(int target) {
		this.target = target;
	}

	@Override
    public void resize(int size) {
		throw new UnsupportedOperationException();
	}

    @Override
    public int size() {
        
        return 0;
    }

}
