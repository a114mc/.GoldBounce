/*
 * MoonLight Hacked Client
 *
 * A free and open-source hacked client for Minecraft.
 * Developed using Minecraft's resources.
 *
 * Repository: https://github.com/randomguy3725/MoonLight
 *
 * Author(s): [Randumbguy & opZywl & lucas]
 */
package net.ccbluex.liquidbounce.utils.animations.impl;


import net.ccbluex.liquidbounce.utils.animations.Animation;
import net.ccbluex.liquidbounce.utils.animations.Direction;

public class EaseInOutQuad extends Animation {

    public EaseInOutQuad(int ms, double endPoint) {
        super(ms, endPoint);
    }

    public EaseInOutQuad(int ms, double endPoint, Direction direction) {
        super(ms, endPoint, direction);
    }

    protected double getEquation(double x) {
        return x < 0.5 ? 2 * Math.pow(x, 2) : 1 - Math.pow(-2 * x + 2, 2) / 2;
    }

}
