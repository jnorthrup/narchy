/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jcog.meter;

/**
 *
 * @author me
 */
@Deprecated public class TemporalMetrics<O> extends Metrics<Double,O> {

    public TemporalMetrics(int historySize) {
        super(historySize);
    }


//    public List<SignalData> getSignalDatas() {
//        List<SignalData> l = getSignals().stream().map(sv -> newSignalData(sv.id)).collect(Collectors.toList());
//
//        return l;
//    }

    /** allows updating with an integer/long time, because it will be converted
     * to double internally
     */
    public void update(long integerTime) {
        update((double)integerTime);
    }


}
