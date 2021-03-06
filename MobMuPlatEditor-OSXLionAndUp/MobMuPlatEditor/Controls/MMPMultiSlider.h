//
//  MMPMultiSlider.h
//  MobMuPlatEditor
//
//  Created by Daniel Iglesia on 3/28/13.
//  Copyright (c) 2013 Daniel Iglesia. All rights reserved.
//

#import "MMPControl.h"

@interface MMPMultiSlider : MMPControl{
    NSView* box;
    NSMutableArray* headViewArray;
    float headWidth;
    int currHeadIndex;
}

@property(nonatomic) int range;
@property(nonatomic) NSMutableArray* valueArray;
@property(nonatomic) NSUInteger outputMode; //0=all values, 1=individual element index+value
@end


