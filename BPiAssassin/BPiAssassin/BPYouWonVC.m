//
//  BPYouWonVC.m
//  BPiAssassin
//
//  Created by Robby Cohen on 4/11/14.
//  Copyright (c) 2014 BP. All rights reserved.
//

#import "BPYouWonVC.h"

@interface BPYouWonVC ()

@end

@implementation BPYouWonVC

+ (id)allocWithRouterParams:(NSDictionary *)params {
    UIStoryboard *storyboard = [UIStoryboard storyboardWithName:@"Main_iPhone" bundle:[NSBundle mainBundle]];
    BPYouWonVC *instance = [storyboard instantiateViewControllerWithIdentifier:@"BPYouWonVC"];
    
    return instance;
}
- (IBAction)retToHomeBtn:(id)sender {
    [[Routable sharedRouter] open:@"homePage"];
    
}

- (id)initWithNibName:(NSString *)nibNameOrNil bundle:(NSBundle *)nibBundleOrNil
{
    self = [super initWithNibName:nibNameOrNil bundle:nibBundleOrNil];
    if (self) {
        // Custom initialization
    }
    return self;
}

- (void)viewDidLoad
{
    [super viewDidLoad];
    // Do any additional setup after loading the view.
    if(self.navigationController) {
        [self.navigationController setNavigationBarHidden:YES];
    }
}

- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

/*
#pragma mark - Navigation

// In a storyboard-based application, you will often want to do a little preparation before navigation
- (void)prepareForSegue:(UIStoryboardSegue *)segue sender:(id)sender
{
    // Get the new view controller using [segue destinationViewController].
    // Pass the selected object to the new view controller.
}
*/

@end
