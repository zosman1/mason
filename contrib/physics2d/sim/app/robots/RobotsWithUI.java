package sim.app.robots;
import sim.engine.*;
import sim.display.*;
import sim.portrayal.continuous.*;
import java.awt.*;
import javax.swing.*;

public class RobotsWithUI extends GUIState
    {
    public Display2D display;
    public JFrame displayFrame;
        
    ContinuousPortrayal2D entityPortrayal = new ContinuousPortrayal2D();
        
    public static void main(String[] args)
        {
        RobotsWithUI simRobots = new RobotsWithUI();
        Console c = new Console(simRobots);
        c.setVisible(true);
        }
    
    public RobotsWithUI() 
        { 
        super(new Robots(System.currentTimeMillis())); 
        }
        
    public RobotsWithUI(SimState state) 
        { 
        super(state); 
        }
        
    public static String getName() 
        { 
        return "Robots"; 
        }
    
    public Object getSimulationInspectedObject() { return state; }
        
    
        
    public void start()
        {
        super.start();
        // set up our portrayals
        setupPortrayals();
        }
        
    public void load(SimState state)
        {
        super.load(state);
        // we now have new grids.  Set up the portrayals to reflect that
        setupPortrayals();
        }
        
    public void setupPortrayals()
        {
        // tell the portrayals what to portray and how to portray them
        entityPortrayal.setField(((Robots)state).fieldEnvironment);
                
        // reschedule the displayer
        display.reset();
                
        // redraw the display
        display.repaint();
        }
        
    public void init(Controller c)
        {
        super.init(c);
                
        // Make the Display2D.  We'll have it display stuff later.
        display = new Display2D(800,800,this); // at 400x400, we've got 4x4 per array position
        displayFrame = display.createFrame();
        c.registerFrame(displayFrame);   // register the frame so it appears in the "Display" list
        displayFrame.setVisible(true);
                
        // attach the portrayals
        display.attach(entityPortrayal,"Robots");
                
        // specify the backdrop color  -- what gets painted behind the displays
        display.setBackdrop(Color.black);  // a dark green
        }
        
    public void quit()
        {
        super.quit();
                
        if (displayFrame!=null) 
            displayFrame.dispose();
                        
        displayFrame = null;  // let gc
        display = null;       // let gc
        }
    }
    
    
    
    
    
