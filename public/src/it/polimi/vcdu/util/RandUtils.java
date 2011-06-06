/**
 * 
 */
package it.polimi.vcdu.util;

import it.polimi.vcdu.sim.ControlParameters;

import java.util.logging.Logger;

import cern.jet.random.Normal;
import cern.jet.random.engine.DRand;
/**
 * @author Valerio Panzica La Manna (panzica@elet.polimi.it)
 *
 */
public class RandUtils {

	protected static Normal normal = 
		new Normal(ControlParameters.getCurrentParameters().getMeanServTime(),ControlParameters.getCurrentParameters().getStandardDeviation(),
				new DRand(ControlParameters.getCurrentParameters().getSeed()));
	public static void reInit(){
		normal = 
			new Normal(ControlParameters.getCurrentParameters().getMeanServTime(),ControlParameters.getCurrentParameters().getStandardDeviation(),
					new DRand(ControlParameters.getCurrentParameters().getSeed()));
	}
	
	
	public static float RandDelay(){
		return RandNormalDelay();
	}

	public static float RandNormalDelay() {
		double nt =	normal.nextDouble();
		if (nt<=0 || nt >=2*ControlParameters.getCurrentParameters().getMeanServTime()){
			Logger.getLogger("it.polimi.vcdu").warning("Abandon too small or too large LocalExeTime: "+ nt 
					+ " regenerate it.");
			nt = RandNormalDelay();
		}
		return (float) nt;

	}

	/**
	 * @param nOutPorts
	 * @return
	 */
	public static int selectPort(int nOutPorts) {
        
		 
//		int port =(int) (new Random(ControlParameters.getCurrentParameters().getSeed()).nextFloat() * nOutPorts);
//		ControlParameters.getCurrentParameters().setSeed(ControlParameters.getCurrentParameters().getSeed()+1000);

		int port = ControlParameters.getCurrentParameters().getRandomObj().nextInt(nOutPorts);
		return port;
	}
	
//	public static int selectComponent(int nComponents){
//		int component =(int) (new Random(ControlParameters.getCurrentParameters().getSeed()).nextFloat() * nComponents);
//		ControlParameters.getCurrentParameters().setSeed(ControlParameters.getCurrentParameters().getSeed()+1000);
//		return component;
//	}

}
