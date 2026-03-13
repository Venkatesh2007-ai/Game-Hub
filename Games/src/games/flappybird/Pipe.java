package games.flappybird;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Rectangle;

public class Pipe {

    private double x;
    private final int width;
    private final int gapCenterY;
    private final int gapHeight;
    private boolean passed;

    public Pipe(double startX, int width, int gapCenterY, int gapHeight) {
        this.x = startX;
        this.width = width;
        this.gapCenterY = gapCenterY;
        this.gapHeight = gapHeight;
        this.passed = false;
    }

    public void update(double deltaSeconds, double speed) {
        x -= speed * deltaSeconds;
    }

    public boolean checkAndMarkPassed(Pig pig) {
        if (!passed && (x + width) < pig.getX()) {
            passed = true;
            return true;
        }
        return false;
    }

    public boolean collides(Pig pig, int groundY) {
        Rectangle pigBounds = pig.getBounds();
        int pipeX = (int) Math.round(x);

        Rectangle topPipeBounds = new Rectangle(pipeX, 0, width, Math.max(0, getGapTop()));
        Rectangle bottomPipeBounds = new Rectangle(
            pipeX,
            getGapBottom(),
            width,
            Math.max(0, groundY - getGapBottom())
        );

        return topPipeBounds.intersects(pigBounds) || bottomPipeBounds.intersects(pigBounds);
    }

    public void render(Graphics2D g2d, int groundY) {
        int drawX = (int) Math.round(x);
        int topPipeHeight = Math.max(0, getGapTop());
        int bottomPipeY = getGapBottom();
        int bottomPipeHeight = Math.max(0, groundY - bottomPipeY);
        int capHeight = 28;
        int capInset = 7;

        GradientPaint bodyPaint = new GradientPaint(
            drawX, 0, new Color(44, 187, 84),
            drawX + width, 0, new Color(20, 128, 58)
        );
        g2d.setPaint(bodyPaint);

        if (topPipeHeight > 0) {
            g2d.fillRect(drawX, 0, width, topPipeHeight);
            int capY = Math.max(0, topPipeHeight - capHeight);
            g2d.fillRect(drawX - capInset, capY, width + (capInset * 2), capHeight);
        }

        if (bottomPipeHeight > 0) {
            g2d.fillRect(drawX, bottomPipeY, width, bottomPipeHeight);
            g2d.fillRect(drawX - capInset, bottomPipeY, width + (capInset * 2), capHeight);
        }

        g2d.setColor(new Color(212, 255, 206, 145));
        if (topPipeHeight > 0) {
            g2d.fillRect(drawX + 8, 0, 7, topPipeHeight);
        }
        if (bottomPipeHeight > 0) {
            g2d.fillRect(drawX + 8, bottomPipeY, 7, bottomPipeHeight);
        }

        g2d.setColor(new Color(19, 96, 45));
        g2d.setStroke(new BasicStroke(2f));
        if (topPipeHeight > 0) {
            g2d.drawRect(drawX, 0, width, topPipeHeight);
            int capY = Math.max(0, topPipeHeight - capHeight);
            g2d.drawRect(drawX - capInset, capY, width + (capInset * 2), capHeight);
        }
        if (bottomPipeHeight > 0) {
            g2d.drawRect(drawX, bottomPipeY, width, bottomPipeHeight);
            g2d.drawRect(drawX - capInset, bottomPipeY, width + (capInset * 2), capHeight);
        }
    }

    public boolean isOffScreen() {
        return (x + width) < -40;
    }

    public double getX() {
        return x;
    }

    public double getRightX() {
        return x + width;
    }

    private int getGapTop() {
        return gapCenterY - (gapHeight / 2);
    }

    private int getGapBottom() {
        return gapCenterY + (gapHeight / 2);
    }
}
