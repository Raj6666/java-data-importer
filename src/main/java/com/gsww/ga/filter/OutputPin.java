package com.gsww.ga.filter;

/**
 * Created by yangpy on 2017/8/29.
 */
public interface OutputPin {

    /**
     * Bind pin.
     *
     * @param pin the pin
     */
    void bindPin(InputPin pin);

    /**
     * Remove pin.
     *
     * @param pin the pin
     */
    void removePin(InputPin pin);

}
