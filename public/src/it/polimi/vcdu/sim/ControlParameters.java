package it.polimi.vcdu.sim;

import java.util.Random;

import cern.jet.random.Normal;
import cern.jet.random.engine.DRand;


public class ControlParameters {
	
	private static ControlParameters currentParameters;
	
	
	

	/**
	 * one arrive in 10 VT in average. used in poisson processes. 
	 */
	private float MeanArrival = 25.0f;   
	
	/**
	 * average serve time. Used in exponential distribution?
	 */
	private float MeanServTime = 50f;
	/**
	 * for normal distribution only
	 */
	private float StandardDeviation = getMeanServTime()/5; 

	private float MaxVirtualTime = 100000;
	
	
	//private float localProcessingTime=50;
	Normal NormalDistributionLocalProcessingTime; 
	private float networkDelay=0f;
	//private float messagePayload= 10;
	private float messagePayloadNotifyFutureCreat=10;
//	private Random RandomObj = new Random(System.currentTimeMillis());
//	private  int Seed = RandomObj.nextInt();   ; //12345;
	
	private int Seed= 12345678;
	private Random RandomObj;
	private Random tempRandom;


//	public static void setCurrentParameters(ControlParameters currentParam) {
//		currentParameters = currentParam;
//	}
	
	protected ControlParameters(){
		RandomObj = new Random(Seed);
		tempRandom = new Random(Seed);
		NormalDistributionLocalProcessingTime = new Normal(this.MeanServTime,this.StandardDeviation, 	new DRand(Seed));
	}

	public static ControlParameters getCurrentParameters() {
		if (currentParameters == null) currentParameters = new ControlParameters();
		return currentParameters;
	}

	public  void setSeed(int seed) {
		Seed = seed;
	}

	public  int getSeed() {
		return Seed;
	}

	public  void setRandomObj(Random randomObj) {
		RandomObj = randomObj;
	}

	public  Random getRandomObj() {
		return RandomObj;
	}

	public  void setMaxVirtualTime(float maxVirtualTime) {
		MaxVirtualTime = maxVirtualTime;
	}

	public  float getMaxVirtualTime() {
		return MaxVirtualTime;
	}

	public  void setStandardDeviation(float standardDeviation) {
		StandardDeviation = standardDeviation;
	}

	public  float getStandardDeviation() {
		return StandardDeviation;
	}

	public  void setMeanServTime(float meanServTime) {
		MeanServTime = meanServTime;
	}

	public  float getMeanServTime() {
		return MeanServTime;
	}

	public  void setMeanArrival(float meanArrival) {
		MeanArrival = meanArrival;
	}

	public  float getMeanArrival() {
		return MeanArrival;
	}

	/**
	 * @return the networkDelay
	 */
	public float getNetworkDelay() {
		return networkDelay;
	}

	/**
	 * @param networkDelay the networkDelay to set
	 */
	public void setNetworkDelay(float networkDelay) {
		this.networkDelay = networkDelay;
	}

	/**
	 * @return the messagePayloadNotifyFutureCreat
	 */
	public float getMessagePayloadNotifyFutureCreat() {
		return messagePayloadNotifyFutureCreat;
	}

	public float getLocalProcessingTime() {
		//return 50f;
		float result = -1f;
		while (result<0 || result>2* this.MeanServTime){
			result = (float) NormalDistributionLocalProcessingTime.nextDouble();
		}
		return result;
	}

	public void reInit(){
		RandomObj = new Random(Seed);
		tempRandom = new Random(Seed);
		NormalDistributionLocalProcessingTime = new Normal(this.MeanServTime,this.StandardDeviation, new DRand(Seed));
	}

	/**
	 * @return the tempRandom
	 */
	public Random getTempRandom() {
		return tempRandom;
	}
}
