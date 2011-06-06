/**
 * 
 */
package it.polimi.vcdu.alg;

/**
 * @author xxm
 *
 */
public enum ApproachToFreeness {
	BLOCKING, //achieving freeness by blocking new requests belonging to unserved transactions.
	WAITING,  //achieving freeness by  waiting opportunistically.
	CONCURVERS; //achieving freeness using concurrent versions. 
}
