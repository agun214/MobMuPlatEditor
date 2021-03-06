package com.iglesiaintermedia.MobMuPlatEditor.controls;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;

import javax.swing.JPanel;

import com.iglesiaintermedia.MobMuPlatEditor.MMPController;

public class MMPTable extends MMPControl {

	 public Color selectionColor;
	 public int mode; // selection, draw
	 private int displayMode; //0=line, 1=fill
	 private float displayRangeLo;
	 private float displayRangeHi;
	 
	int tableSize;
	float[] tableData;
	
	float fR, fG, fB, fA;
	float sR, sG, sB, sA;
	BufferedImage cacheImage;
	BufferedImage cacheImageSelection;
	Graphics2D cacheGraphics;
	Graphics2D cacheGraphicsSelection;
	
	boolean loadedTable;
	boolean _created;
	
	//bookmarks
	Point touchDownPoint,lastPoint;
	int lastTableIndex;
	
	//JPanel _grayOutPanel;
	
	//copy constructor
	public MMPTable(MMPTable otherTable){
		this(otherTable.getBounds());//normal constructor
		this.address=otherTable.address;
		this.setColor(otherTable.getColor());
		this.setHighlightColor(otherTable.getHighlightColor());
		this.mode = otherTable.mode;
		this.selectionColor = otherTable.selectionColor;
		this.displayMode = otherTable.displayMode;
		this.displayRangeLo = otherTable.displayRangeLo;
		this.displayRangeHi = otherTable.displayRangeHi;
	}
	
	public MMPTable(Rectangle frame){
		super();
		this.setAddress("/myTable");
		_created = true;
		this.setSelectionColor(new Color(1f,1f,1f,.5f));
		this.displayRangeLo = -1;
		this.displayRangeHi = 1;
		
		this.addMouseListener(this);
		this.addMouseMotionListener(this);
		
		//TODO fix super.setColor so I don't have to do this...
		this.setColor(this.getColor());
		this.setHighlightColor(this.getHighlightColor());
		
		this.setBounds(frame);
		//paintRect(.2f, .2f, .8f, .8f);
		
	} 
	
	public void setAddress(String newAddress){
		super.setAddress(newAddress);
		if(_created && editingDelegate!=null)loadTable();//checks to prevent load when we don't have delegate set yet...though not a prob?
	}
	
	public void loadTable(){
		loadedTable = false;
		Object[] args = new Object[]{MMPController.cachePathWithAddress(address), address};
		editingDelegate.sendMessage("/system/requestTable", args);
	}
	
	public void setBounds(Rectangle frame){
		super.setBounds(frame);
	    createBitmapContext(frame); 
		//this.repaint();
	    draw();
	}
	
	private void createBitmapContext(Rectangle frame){
		cacheImage = new BufferedImage(frame.width, frame.height, BufferedImage.TYPE_INT_ARGB);
		cacheGraphics = cacheImage.createGraphics();
		cacheGraphics.setStroke(new BasicStroke(2));
		cacheGraphics.setBackground(new Color(255, 255, 255, 0));
		
		cacheImageSelection = new BufferedImage(frame.width, frame.height, BufferedImage.TYPE_INT_ARGB);
		cacheGraphicsSelection = cacheImageSelection.createGraphics();
		cacheGraphicsSelection.setBackground(new Color(255, 255, 255, 0));
		
	}
	
	public void setColor(Color newColor){
		super.setColor(newColor);
		this.setBackground(newColor);
	}
	
	public void setHighlightColor(Color newColor){
		super.setHighlightColor(newColor);
		float[] compArray = new float[4];
		newColor.getComponents(compArray);
		fR=compArray[0];
		fG=compArray[1];
		fB=compArray[2];
		fA=compArray[3];
		
		//this.repaint();//without this, horiz view peeked on top of the edithandle border..
	}
	
	public void setSelectionColor(Color newColor) {
		selectionColor = newColor;
		float[] compArray = new float[4];
		newColor.getComponents(compArray);
		sR=compArray[0];
		sG=compArray[1];
		sB=compArray[2];
		sA=compArray[3];
	}
	public Color getSelectionColor(){
		return selectionColor;
	}
	
	public void setMode(int newMode){
		mode = newMode;
		//clear selection
		cacheGraphicsSelection.clearRect(0,0,getWidth(),getHeight());
		this.repaint();
		
	}
	public int getMode(){
		return mode;
	}
	
	//drawing
	
	void draw(){
		drawRange(0, tableSize-1);
	}
	
	void drawRange(int indexA, int indexB) {
		if(tableData==null)return;
		// check for div by zero
		if (displayRangeHi == displayRangeLo)return;
		  
		int padding=3;
		
		cacheGraphics.setColor(new Color(fR,fG,fB,fA) );
		GeneralPath polygon =   new GeneralPath(GeneralPath.WIND_EVEN_ODD);
		//float x = ((Float)(messageArray.get(0))).floatValue() * getWidth();
		//float y = ((Float)(messageArray.get(1))).floatValue() * getHeight();
		//polygon.moveTo(0,0); //prob unneccesary
		
		int indexDrawPointA = (int)((float)Math.min(indexA,indexB)/tableSize*this.getWidth())-padding;
		indexDrawPointA = Math.min(Math.max(indexDrawPointA,0),this.getWidth()-1);
		int indexDrawPointB = (int)((float)(Math.max(indexA,indexB)+1)/(tableSize)*this.getWidth())+padding;
		indexDrawPointB = Math.min(Math.max(indexDrawPointB,0),this.getWidth()-1);
		//System.out.print("index AB drawpoint AB "+ indexA+" "+indexB+" "+indexDrawPointA+" "+indexDrawPointB);
		//Rectangle rect =  new Rectangle(indexDrawPointA, 0, indexDrawPointB-indexDrawPointA, this.getHeight());
		cacheGraphics.clearRect(indexDrawPointA, 0, indexDrawPointB-indexDrawPointA, this.getHeight());
		
		for(int i=indexDrawPointA; i<=indexDrawPointB; i++){//for each pixel
		    float x = (float)i;//(float)i/self.frame.size.width;
		    int index = (int)((float)i/this.getWidth()*tableSize);
		    
		    //if touch down one point, make sure that point is represented in redraw and not skipped over
		    int prevIndex = (int)((float)(i-1)/this.getWidth()*tableSize);
		    if(indexA==indexB && indexA<index && indexA>prevIndex) index = indexA;
		    
		    float y = tableData[index];
		    //float unflippedY = (1-((y+1)/2)) *this.getHeight();
		    // Scale lo to hi to flipped 0 to frame height.
		    float unflippedY = 1-( (y-displayRangeLo)/(displayRangeHi - displayRangeLo));
		    unflippedY *= getHeight();
		   //System.out.println("i:"+ i+" x:"+x+" index:"+index+" y:"+y+" unflippd:"+unflippedY);
		    if(i==indexDrawPointA){
		    	polygon.moveTo( x,unflippedY);
		    }
		    else {
		    	polygon.lineTo( x, unflippedY);
		    }
		  }  
		
	    
	    if (displayMode == 0) { //line
	    	//polygon.closePath();
	    	cacheGraphics.draw(polygon);
	    } else if (displayMode == 1) { //fill
	    	// add outer points
	    	float yPointOfTableZero = 1 - ((0 -displayRangeLo)/(displayRangeHi - displayRangeLo));
	        yPointOfTableZero *= getHeight();
	        polygon.lineTo(indexDrawPointB, yPointOfTableZero);
	    	polygon.lineTo(indexDrawPointA, yPointOfTableZero);
	    	polygon.closePath(); // draws line back to initial .moveTo()
	    	cacheGraphics.fill(polygon);
	    	cacheGraphics.draw(polygon);// fill and stroke.
	    }
	    
	    
	    Rectangle newRect2 = new Rectangle( indexDrawPointA, 0, Math.abs(indexDrawPointB-indexDrawPointA), this.getHeight() );
		this.repaint(newRect2);
	}
	
	void drawHighlightRange(Point pointA, Point pointB) {//unnormalized
		cacheGraphicsSelection.setColor(new Color(sR,sG,sB,sA) );
		Rectangle newRect = new Rectangle(Math.min(pointA.x, pointB.x), 0, Math.abs(pointB.x-pointA.x), getHeight() );
		cacheGraphicsSelection.clearRect(0,0,getWidth(),getHeight());
		cacheGraphicsSelection.fill(newRect);
		this.repaint();
		
	}
	
	void sendRangeMessage(int fromIndex, int toIndex){
		editingDelegate.sendMessage(address, new Object[]{"range",new Integer(fromIndex), new Integer(toIndex)});
	}
	
	void sendSetTableMessage(int indexA, float valA, int indexB, float valB) {
		Object[] args = new Object[]{address,new Integer(indexA), new Float(valA), new Integer(indexB), new Float(valB)};
		 editingDelegate.sendMessage("/system/setTable", args); 
		  //look for all other tables with same address, and just refresh?
	}
	
	//mouse
	public void mousePressed(MouseEvent e) {
		//System.out.print(e.getClickCount() + " click(s)");
		super.mousePressed(e);
		
		if(!editingDelegate.isEditing() && loadedTable){
			lastPoint = e.getPoint();
			touchDownPoint = lastPoint;
		    if(mode==0) { //select
		    	float normalizedXA = (float)touchDownPoint.x/this.getWidth();
		        normalizedXA = Math.max(Math.min(normalizedXA,1),0);//touch down should always be in bounds, this is prob unnecc
		        int downTableIndex = (int)(normalizedXA*tableSize);
		        downTableIndex = Math.min(downTableIndex, tableSize-1);//clip to max index, prob unnecc here
		        
		        sendRangeMessage(downTableIndex, downTableIndex);
		        drawHighlightRange(lastPoint,new Point(lastPoint.x+1, lastPoint.y));
		    }
		    else {//draw
		    	float normalizedX = (float)lastPoint.x/getWidth();
		        int touchDownTableIndex = (int)(normalizedX*tableSize);
		        touchDownTableIndex = Math.min(touchDownTableIndex, tableSize-1);//clip to max index, prob unnecc here
		        lastTableIndex = touchDownTableIndex;
		        
		        float normalizedY = (float)lastPoint.y/getHeight();//change to -1 to 1
		        //float flippedY = (1-normalizedY)*2-1;
		        float flippedY = (1 - normalizedY)*(displayRangeHi - displayRangeLo) + displayRangeLo;

		        //NSLog(@"touchDownTableIndex %d", touchDownTableIndex);
		        
		        tableData[touchDownTableIndex] = flippedY;//check bounds
		        drawRange(touchDownTableIndex, touchDownTableIndex); 
		        
		        //send one element
		        sendSetTableMessage(touchDownTableIndex, flippedY, touchDownTableIndex, flippedY);
		    }
	    }
	}

	@Override
	public void mouseClicked(MouseEvent e) {
	
	}

	@Override
	public void mouseEntered(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseExited(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		super.mouseReleased(e);
	    if(!editingDelegate.isEditing()){
	    	
	    }
		
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		super.mouseDragged(e);
		
		if(!editingDelegate.isEditing() && loadedTable){
			Point dragPoint = e.getPoint();
			
		    if(mode==0) { //select
		    	float normalizedXA = (float)touchDownPoint.x/this.getWidth();
		        normalizedXA = Math.max(Math.min(normalizedXA,1),0);//touch down should always be in bounds, this is prob unnecc
		        int dragTableIndexA = (int)(normalizedXA*tableSize);
		        dragTableIndexA = Math.min(dragTableIndexA, tableSize-1);//clip to max index
		        
		        float normalizedXB = (float)dragPoint.x/this.getWidth();
		        normalizedXB = Math.max(Math.min(normalizedXB,1),0);//touch down should always be in bounds, this is prob unnecc
		        int dragTableIndexB = (int)(normalizedXB*tableSize);
		        dragTableIndexB = Math.min(dragTableIndexB, tableSize-1);//clip to max index
		        
		        sendRangeMessage(Math.min(dragTableIndexA, dragTableIndexB), Math.max(dragTableIndexA, dragTableIndexB));
		        drawHighlightRange(touchDownPoint, dragPoint);
		    }
		    else {
		    	float normalizedX = (float)dragPoint.x/getWidth();
		        normalizedX = Math.max(Math.min(normalizedX,1),0);
		        int dragTableIndex = (int)(normalizedX*tableSize);
		        dragTableIndex = Math.min(dragTableIndex, tableSize-1);//clip to max index
		        
		        float normalizedY = (float)dragPoint.y/getHeight();//change to -1 to 1
		        normalizedY = Math.max(Math.min(normalizedY,1),0);
		        //float flippedY = (1-normalizedY)*2-1;
		        float flippedY = (1 - normalizedY)*(displayRangeHi - displayRangeLo) + displayRangeLo;

		        //compute size, including self but not prev
		        int traversedElementCount = Math.abs(dragTableIndex-lastTableIndex);
		        if(traversedElementCount==0)traversedElementCount=1;
		        //float* touchValArray = (float*)malloc(traversedElementCount*sizeof(float));
		        
		        tableData[dragTableIndex] = flippedY;
		        //==================just for local representation
		        //just one
		        if(traversedElementCount==1) {
		          drawRange(dragTableIndex, dragTableIndex);
		        } else {
		          int minIndex = Math.min(lastTableIndex, dragTableIndex);
		          int maxIndex = Math.max(lastTableIndex, dragTableIndex);
		          
		          float minValue = tableData[minIndex];
		          float maxValue = tableData[maxIndex];
		          //NSLog(@"skip within %d (%.2f) to %d(%.2f)", minTouchIndex, [[_valueArray objectAtIndex:minTouchIndex] floatValue], maxTouchIndex, [[_valueArray objectAtIndex:maxTouchIndex] floatValue]);
		          for(int i=minIndex+1;i<=maxIndex;i++){
		            float percent = ((float)(i-minIndex))/(maxIndex-minIndex);
		            float interpVal = (maxValue - minValue) * percent  + minValue ;
		            //NSLog(@"%d %.2f %.2f", i, percent, interpVal);
		            tableData[i]=interpVal;
		            //touchValArray[i-(minIndex+1)]=interpVal;
		          }
		          drawRange(minIndex, maxIndex);
		        }
		        //=======send end points to pd wrapper to do its own interp
		        int minIndex = Math.min(lastTableIndex, dragTableIndex);
		        int maxIndex = Math.max(lastTableIndex, dragTableIndex);
		        float minValue = tableData[minIndex];
		        float maxValue = tableData[maxIndex];
		        sendSetTableMessage(minIndex, minValue, maxIndex, maxValue);
		        //====
		        
		        lastTableIndex = dragTableIndex;
		    }
		    lastPoint = dragPoint;
	    }
	}

	public void setDisplayMode(int newMode) {
		this.displayMode = newMode;
		draw();
	}
	public int getDisplayMode() {
		return displayMode;
	}
	public void setDisplayRangeLo(float newRangeLo) {
		this.displayRangeLo = newRangeLo;
		draw();
	}
	public float getDisplayRangeLo() {
		return displayRangeLo;
	}
	public void setDisplayRangeHi(float newRangeHi) {
		this.displayRangeHi = newRangeHi;
		draw();
	}
	public float getDisplayRangeHi() {
		return displayRangeHi;
	}
	
	public void setDisplayRangeConstant(int displayRangeConstant) {
		if (displayRangeConstant == 0) {// polar -1 to 1
		    displayRangeLo = -1;
		    displayRangeHi = 1;
		  } else if (displayRangeConstant == 1) {
		    displayRangeLo = 0;
		    displayRangeHi = 1;
		  }
		  draw();
	}
	public int getDisplayRangeConstant() {
		if (displayRangeLo == -1 && displayRangeHi == 1) {
			return 0;
		} else if (displayRangeLo == 0 && displayRangeHi == 1) {
			return 1;
		} else {
			return -1;
		}
	}
	
	/*public void clear(){
		cacheGraphics.setBackground(new Color(255, 255, 255, 0));
		cacheGraphics.clearRect(0,0,getWidth(), getHeight());
		this.repaint();
	}*/
	protected void paintComponent(Graphics g) {
		
        super.paintComponent(g);
        int width = getWidth();
        int height = getHeight();
        
        Graphics2D graphics = (Graphics2D) g;
        graphics.setColor(getBackground());
        graphics.fillRect(0, 0, width, height);
        graphics.drawImage(cacheImageSelection, 0, 0, null);
        graphics.drawImage(cacheImage, 0, 0, null);
        
	}
	
	public void receiveList(ArrayList<Object> messageArray){
		super.receiveList(messageArray);
		if(messageArray.size()==1 && (messageArray.get(0) instanceof String) && messageArray.get(0).equals("clearSelection") ){
			cacheGraphicsSelection.clearRect(0,0,getWidth(),getHeight());
			this.repaint();
		}
		else if(messageArray.size()==1 && (messageArray.get(0) instanceof String) && messageArray.get(0).equals("done")){
			//System.out.print(this.getAddress()+" done ");
			readFileToArray();
			loadedTable = true;
			draw();
		}
		else if(messageArray.size()==1 && (messageArray.get(0) instanceof String) && messageArray.get(0).equals("refresh") ){
			loadTable();
		}
	}
	
	void readFileToArray() {
		String path = MMPController.cachePathWithAddress(this.getAddress());
		File tempFile = new File(path);
		if(!tempFile.exists()) return;//error?
		
		ArrayList<String> stringList = new ArrayList<String>();
		BufferedReader reader;
		try {
			reader = new BufferedReader( new FileReader (tempFile));
			String         line = null;
		    while( ( line = reader.readLine() ) != null ) {
		    	stringList.add(line);
		    }
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//System.out.println("string list size "+stringList.size());
		tableSize = stringList.size();//PD adds extra line, but reader doesn't read it.
		tableData = new float[tableSize];
		int i=0;
		for (String line : stringList) {
			tableData[i++] = Float.parseFloat(line);
		}
		
	}
	
	public void setEnabled(boolean enabled){
		super.setEnabled(enabled);
		Color c = this.isEnabled() ? getColor() : getDisabledColor();
		this.setBackground(c);
		Color h = this.isEnabled() ? getHighlightColor() : getDisabledHighlightColor();
		float[] compArray = new float[4];
		h.getComponents(compArray);
		fR=compArray[0];
		fG=compArray[1];
		fB=compArray[2];
		fA=compArray[3];
		this.draw();
		
	}
}
