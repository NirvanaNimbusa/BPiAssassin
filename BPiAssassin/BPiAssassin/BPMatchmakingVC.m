//
//  BPMatchmakingVC.m
//  BPiAssassin
//
//  Created by John Rozier on 2/15/14.
//  Copyright (c) 2014 BP. All rights reserved.
//

#import "BPMatchmakingVC.h"
#import "BPAPIClient.h"
#import "BPNotifications.h"
#import "BPAPIClientObjects.h"
@interface BPMatchmakingVC ()

@end

/* Note: this class is going to be replaced most likely with a GameCenter
 *  matchmaking VC
 *
 *  This class currently just calls create game automatically with default data
 *      This will need to be fixed in the future.
 *
 */


@implementation BPMatchmakingVC

- (id)initWithNibName:(NSString *)nibNameOrNil bundle:(NSBundle *)nibBundleOrNil
{
    self = [super initWithNibName:nibNameOrNil bundle:nibBundleOrNil];
    if (self) {
        // Custom initialization
    }
    return self;
}
- (IBAction)createGameBtnPress:(id)sender {
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    NSString* robbysUUID = @"000b9508-2046-4c67-000b-950820464d44";
    NSString* johnsUUID = @"000b93b7-ee30-75b4-000b-93b7ee3076f4";
    
    __block id gameCreated = [[NSNotificationCenter defaultCenter] addObserverForName:kGameCreatedNotification object:[BPAPIClient sharedAPIClient] queue:nil usingBlock:^(NSNotification *note) {
        [[NSNotificationCenter defaultCenter] removeObserver:gameCreated];

        BPAPIGameCreated *gameCreated = [[note userInfo] objectForKey:@"event"];
        
        UIAlertView *alert = [[UIAlertView alloc] initWithTitle:@"Yay game created successfully" message:[NSString stringWithFormat:@"Game id: %@, number of Images: %d",[gameCreated gameId],[gameCreated images].count] delegate:self cancelButtonTitle:@"Close" otherButtonTitles:nil];
        [alert show];
        
        [defaults setObject:[gameCreated gameId] forKey:@"gameUUID"];
        [defaults synchronize];
        
        [[BPAPIClient sharedAPIClient] startGameWithGameId:[defaults objectForKey:@"gameUUID"] withMeanImage:@"MeanImage000" withCovarEigen:@"CovarEigen00" withWorkFunctEigen:@"WorkFunctionEigen000" withProjectedImages:@"ProjectedImages0"];
        
    }];
    __block id gameStarted = [[NSNotificationCenter defaultCenter] addObserverForName:kGameStartedNotification object:[BPAPIClient sharedAPIClient] queue:nil usingBlock:^(NSNotification *note) {
        [[NSNotificationCenter defaultCenter] removeObserver:gameStarted];
        
        UIAlertView *alert = [[UIAlertView alloc] initWithTitle:@"Yay game started successfully" message:[NSString stringWithFormat:@"Game id: %@",[defaults objectForKey:@"gameUUID"]] delegate:self cancelButtonTitle:@"Close" otherButtonTitles:nil];
        [alert show];
                            
       
        [[BPAPIClient sharedAPIClient] getTargetForGameId:[defaults objectForKey:@"gameUUID"] forUserId:[defaults objectForKey:@"myUUID"]];
        
    }];
    
    __block id targetReceived = [[NSNotificationCenter defaultCenter] addObserverForName:kTargetReceivedNotification object:[BPAPIClient sharedAPIClient] queue:nil usingBlock:^(NSNotification *note) {
        [[NSNotificationCenter defaultCenter] removeObserver:targetReceived];
        
        BPAPINewTargetReceived *targetReceived = [[note userInfo]objectForKey:@"event"];
        
        [defaults setObject:[targetReceived targetCodename] forKey:@"targetCodename"];
        [defaults setObject:[targetReceived targetId] forKey:@"targetUUID"];
        [defaults setObject:[targetReceived targetThumbnail] forKey:@"targetThumbnail"];
        [defaults synchronize];
        
        [defaults setBool:YES forKey:@"gameInProgress"];
        [defaults synchronize];
        
        [self performSegueWithIdentifier:@"gameSucessfullyStarted" sender:self];
    }];

    
    [[BPAPIClient sharedAPIClient] createGameWithHostId:robbysUUID withAllPlayersId:@[robbysUUID, johnsUUID]];
    
}

- (void)viewDidLoad
{
    [super viewDidLoad];
}

- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
}




@end
