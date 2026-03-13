package games.flappybird;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;

public class Pig {

    private static final double GRAVITY = 1450.0;
    private static final double FLAP_VELOCITY = -420.0;
    private static final double MAX_FALL_SPEED = 760.0;
    private static final double MAX_UP_TILT = -0.56;
    private static final double MAX_DOWN_TILT = 0.84;

    private static final int COLLISION_INSET_X = 8;
    private static final int COLLISION_INSET_TOP = 6;
    private static final int COLLISION_INSET_BOTTOM = 8;
    private static final int VISIBILITY_MARGIN = 12;

    private final int width;
    private final int height;

    private double x;
    private double y;
    private double velocityY;
    private double wingPhase;
    private double wingBurstTimer;
    private double tilt;

    public Pig(double startX, double startY) {
        this.width = 58;
        this.height = 52;
        reset(startX, startY);
    }

    public void reset(double startX, double startY) {
        x = startX;
        y = startY;
        velocityY = 0.0;
        wingPhase = 0.0;
        wingBurstTimer = 0.0;
        tilt = 0.0;
    }

    public void flap() {
        velocityY = FLAP_VELOCITY;
        wingBurstTimer = 0.26;
        wingPhase += 0.7;
    }

    public void update(double deltaSeconds) {
        velocityY = Math.min(MAX_FALL_SPEED, velocityY + (GRAVITY * deltaSeconds));
        y += velocityY * deltaSeconds;

        wingBurstTimer = Math.max(0.0, wingBurstTimer - deltaSeconds);
        double wingSpeed = wingBurstTimer > 0.0 ? 30.0 : 13.0;
        wingPhase += deltaSeconds * wingSpeed;

        double targetTilt = Math.max(MAX_UP_TILT, Math.min(MAX_DOWN_TILT, (velocityY / MAX_FALL_SPEED) * 1.05));
        tilt += (targetTilt - tilt) * Math.min(1.0, deltaSeconds * 13.0);
    }

    public void render(Graphics2D g2d) {
        Graphics2D copy = (Graphics2D) g2d.create();

        double centerX = x + (width / 2.0);
        double centerY = y + (height / 2.0);
        copy.rotate(tilt, centerX, centerY);

        int drawX = (int) Math.round(x);
        int drawY = (int) Math.round(y);
        int bodyX = drawX + 6;
        int bodyY = drawY + 10;
        int bodyW = 46;
        int bodyH = 34;

        copy.setColor(new Color(0, 0, 0, 48));
        copy.fillOval(drawX + 8, drawY + 43, 42, 7);

        double flap = 0.5 + (Math.sin(wingPhase) * 0.5);
        int wingLift = (int) Math.round((1.0 - flap) * 6.0);
        int wingHeight = 9 + (int) Math.round(flap * 7.0);
        int wingY = bodyY + 8 - wingLift;

        copy.setColor(new Color(255, 218, 233));
        copy.fillOval(drawX + 3, wingY, 13, wingHeight);
        copy.fillOval(drawX + 42, wingY, 13, wingHeight);
        copy.setColor(new Color(217, 150, 173));
        copy.drawOval(drawX + 3, wingY, 13, wingHeight);
        copy.drawOval(drawX + 42, wingY, 13, wingHeight);

        copy.setColor(new Color(255, 196, 217));
        copy.fillOval(drawX + 14, drawY + 1, 10, 13);
        copy.fillOval(drawX + 28, drawY + 1, 10, 13);
        copy.setColor(new Color(245, 158, 186));
        copy.fillOval(drawX + 17, drawY + 6, 4, 6);
        copy.fillOval(drawX + 31, drawY + 6, 4, 6);

        int ribbonX = drawX + 9;
        int ribbonY = drawY + 0;
        copy.setColor(new Color(241, 88, 145));
        copy.fillOval(ribbonX, ribbonY + 2, 10, 8);
        copy.fillOval(ribbonX + 9, ribbonY, 10, 8);
        copy.fillOval(ribbonX + 7, ribbonY + 3, 7, 7);
        copy.setColor(new Color(182, 57, 110));
        copy.drawOval(ribbonX, ribbonY + 2, 10, 8);
        copy.drawOval(ribbonX + 9, ribbonY, 10, 8);
        copy.drawOval(ribbonX + 7, ribbonY + 3, 7, 7);

        copy.setColor(new Color(255, 188, 209));
        copy.fillOval(bodyX, bodyY, bodyW, bodyH);
        copy.setColor(new Color(224, 132, 161));
        copy.setStroke(new BasicStroke(2f));
        copy.drawOval(bodyX, bodyY, bodyW, bodyH);

        copy.setColor(new Color(255, 220, 232, 140));
        copy.fillOval(bodyX + 8, bodyY + 7, 16, 9);

        copy.setColor(Color.WHITE);
        copy.fillOval(bodyX + 20, bodyY + 8, 8, 8);
        copy.setColor(new Color(44, 28, 44));
        copy.fillOval(bodyX + 23, bodyY + 11, 4, 4);

        copy.setColor(new Color(255, 166, 192));
        copy.fillOval(bodyX + 33, bodyY + 10, 16, 12);
        copy.setColor(new Color(225, 120, 153));
        copy.drawOval(bodyX + 33, bodyY + 10, 16, 12);
        copy.fillOval(bodyX + 38, bodyY + 14, 2, 4);
        copy.fillOval(bodyX + 42, bodyY + 14, 2, 4);

        copy.setColor(new Color(247, 149, 179, 170));
        copy.fillOval(bodyX + 4, bodyY + 15, 7, 6);
        copy.fillOval(bodyX + 31, bodyY + 15, 7, 6);

        copy.setColor(new Color(224, 132, 161));
        copy.drawArc(drawX + 2, bodyY + 16, 10, 9, 210, 260);
        copy.drawArc(drawX + 1, bodyY + 18, 6, 6, 210, 260);

        copy.setColor(new Color(250, 168, 194));
        copy.fillRoundRect(bodyX + 8, bodyY + 30, 7, 8, 3, 3);
        copy.fillRoundRect(bodyX + 17, bodyY + 30, 7, 8, 3, 3);
        copy.fillRoundRect(bodyX + 28, bodyY + 30, 7, 8, 3, 3);
        copy.fillRoundRect(bodyX + 37, bodyY + 30, 7, 8, 3, 3);
        copy.setColor(new Color(208, 122, 151));
        copy.drawRoundRect(bodyX + 8, bodyY + 30, 7, 8, 3, 3);
        copy.drawRoundRect(bodyX + 17, bodyY + 30, 7, 8, 3, 3);
        copy.drawRoundRect(bodyX + 28, bodyY + 30, 7, 8, 3, 3);
        copy.drawRoundRect(bodyX + 37, bodyY + 30, 7, 8, 3, 3);

        copy.dispose();
    }

    public Rectangle getBounds() {
        return new Rectangle(
            (int) Math.round(x + COLLISION_INSET_X),
            (int) Math.round(y + COLLISION_INSET_TOP),
            width - (COLLISION_INSET_X * 2),
            height - (COLLISION_INSET_TOP + COLLISION_INSET_BOTTOM)
        );
    }

    public int getVisibilityMargin() {
        return VISIBILITY_MARGIN;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public double getBottom() {
        return y + height;
    }

    public void setY(double y) {
        this.y = y;
    }
}
