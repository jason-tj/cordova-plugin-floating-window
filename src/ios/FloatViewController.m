//
//  FloatViewController.m
//
//
//  Created by noah on 2022/1/13.
//

#import "FloatViewController.h"
#import <AVKit/AVKit.h>

#import "FloatingWindowPlugin.h"

@interface FloatViewController () <AVPictureInPictureControllerDelegate>

@property(nonatomic,strong) AVPlayer * player;

@property (nonatomic ,strong)   UIWindow *window;
@property (nonatomic ,strong)   UIView *playerView;

@property(nonatomic,strong) NSString * flg;

@property(nonatomic,strong) AVPlayerItem * playerItem;

 

@property(nonatomic,strong) AVPictureInPictureController * picController;


@property (nonatomic ,strong) FloatingWindowPlugin *pluginCallBack;

@end

static float  paly_times_cur;
static int is_speed;
static const NSString *ItemStatusContext;

@implementation FloatViewController

- (void)viewDidLoad {
    [super viewDidLoad];
 
}

- (void)setUpPlayer: (NSString *)video_url  i_times_cur:(float )i_times_cur   i_landscape:(NSInteger )i_landscape  i_is_speed:(NSInteger )i_is_speed
{
    [[AVAudioSession sharedInstance] setCategory:AVAudioSessionCategoryPlayback error:nil];
    [[AVAudioSession sharedInstance] setActive:YES error:nil];
    
    _pluginCallBack = [[FloatingWindowPlugin alloc] init];
    
    _window = [[UIWindow alloc] initWithFrame:[UIScreen mainScreen].bounds];
    //创建uiview对象
    _playerView = [[UIView alloc] init];
    paly_times_cur = i_times_cur;
    if(i_landscape==1){ //横屏
      [self.playerView setFrame:CGRectMake(100,100,175,102)];
    } else {
      [self.playerView setFrame:CGRectMake(100,100,102,175)];
    }
    _playerView.backgroundColor = [UIColor whiteColor];
    [self.playerView setTag:20];
    
    [_window addSubview:_playerView];
     
    
    AVAsset *asset = [AVAsset assetWithURL: [NSURL URLWithString:video_url]];
    AVPlayerItem * playerItem = [[AVPlayerItem alloc] initWithAsset:asset  automaticallyLoadedAssetKeys:@[@"duration"]];
  
     
    //添加监听
    [playerItem addObserver:self forKeyPath:@"loadedTimeRanges" options:NSKeyValueObservingOptionNew context:nil];
    [playerItem addObserver:self forKeyPath:@"status" options:NSKeyValueObservingOptionNew context:nil];

     
    self.player = [AVPlayer   playerWithPlayerItem:playerItem];
    
    AVPlayerLayer * layer = [AVPlayerLayer   playerLayerWithPlayer:self.player];
    
    layer.frame = self.playerView.bounds;
    layer.backgroundColor = [UIColor blueColor].CGColor;
    NSLog(@"%@",NSStringFromCGRect(self.view.bounds));
    [self.playerView.layer addSublayer:layer];

    NSLog(@"---------------%@",NSStringFromCGRect(layer.bounds));

    self.picController = [[AVPictureInPictureController alloc] initWithPlayerLayer:layer];
    self.picController.delegate = self;
    is_speed = i_is_speed;
    if(is_speed !=1 )
    {
        self.picController.requiresLinearPlayback = true; //隐藏快进按钮
    }
    
    //给AVPlayerItem添加播放完成通知
        [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(playbackFinished:) name:AVPlayerItemDidPlayToEndTimeNotification object:self.player.currentItem];
    
}
 

//监听视频加载回调
- (void)observeValueForKeyPath:(NSString *)keyPath ofObject:(id)object change:(NSDictionary<NSString *,id> *)change context:(void *)context
{
    AVPlayerItem *playerItem = (AVPlayerItem *)object;

    if ([keyPath isEqualToString:@"loadedTimeRanges"]){
        
    }else if ([keyPath isEqualToString:@"status"]){
        if (playerItem.status == AVPlayerItemStatusReadyToPlay){
            //NSLog(@"playerItem is ready");
            [self.pluginCallBack  sendCmd: @"" ];
          
        } else{
            NSLog(@"load break");
        }
    }
}


- (void) show {
    if (self.picController.isPictureInPicturePossible) {
        self.flg = @"show";
        [self.picController startPictureInPicture];
    }
    else
    {
        NSLog(@"picture is not possible");
    }
    
}

- (void) close{
    if(self.picController.isPictureInPictureActive){
        self.flg = @"close";
        [self.picController stopPictureInPicture];
        //[self sendCurTimeMsg];
    }
}

-(void)viewDidAppear:(BOOL)animated
{

    [super viewDidAppear:animated];
}

//跳转到指定的秒数
-(void)jumptoValue {
    if(paly_times_cur > 0){
     CMTime changedTime = CMTimeMakeWithSeconds( paly_times_cur / 1000, 1);
        [self.player seekToTime:changedTime completionHandler:^(BOOL finished) {
         
        }];
    }
 
}

 
 
-(void) sendCurTimeMsg {
    [self.player pause];
    [self.player setRate: 0];
    
    CMTime time = self.player.currentTime;
    NSTimeInterval cur_time =  time.value / time.timescale;
    int seconds = ((int)cur_time) * 1000 * 1000; //微秒 
    
    //释放资源
    [self.playerItem removeObserver:self forKeyPath:@"status"];
    [self.playerItem removeObserver:self forKeyPath:@"loadedTimeRanges"];
    [self.playerItem cancelPendingSeeks ];
    [self.playerItem.asset cancelLoading];
    self.playerItem = nil;
    [self.player.currentItem.asset cancelLoading];
    [self.player.currentItem cancelPendingSeeks];
    [self.player replaceCurrentItemWithPlayerItem: nil];
    self.player = nil;
    
    //self.playerView = nil;
    //self.picController = nil;
    
    [self.playerView removeFromSuperview];
    //[self removeFromParentViewController];
    
    [self.pluginCallBack  sendCmd: [NSString stringWithFormat:@"%d", seconds ]];
   
}

-(void)playbackFinished:(NSNotification *)notification{
    NSLog(@"视频播放完成");
    self.flg = @"show";
    is_speed = 1;
    self.picController.requiresLinearPlayback = false; //显示快进按钮
    [self.pluginCallBack  sendCmd :@"-100" ];// -100: 播放完毕
    
}



#pragma mark - delegate

- (void)pictureInPictureControllerWillStartPictureInPicture:(AVPictureInPictureController *)pictureInPictureController
{
    //UIWindow *firstWindow = [UIApplication sharedApplication].windows.firstObject;
    //[firstWindow addSubview:self.playerView];
    //[self.playerView mas_makeConstraints:^(MASConstraintMaker *make) {
    //make.edges.mas_equalTo(firstWindow);
    //}];
}


- (void)pictureInPictureControllerDidStartPictureInPicture:(AVPictureInPictureController *)pictureInPictureController
{
//开启
    
    [self.player play];
    [self jumptoValue];
   
}


- (void)pictureInPictureController:(AVPictureInPictureController *)pictureInPictureController failedToStartPictureInPictureWithError:(NSError *)error
{
    NSLog(@"%@",error);
}


- (void)pictureInPictureControllerWillStopPictureInPicture:(AVPictureInPictureController *)pictureInPictureController
{
    //停止ing
   [self sendCurTimeMsg];
}
- (void)pictureInPictureControllerDidStopPictureInPicture:(AVPictureInPictureController *)pictureInPictureController
{
//停止
 
    
}
- (void)pictureInPictureController:(AVPictureInPictureController *)pictureInPictureController restoreUserInterfaceForPictureInPictureStopWithCompletionHandler:(void (^)(BOOL restored))completionHandler
{
    //回到APP
    if([self.flg isEqual:@"show"]){
        
        
    // [self.pluginCallBack  sendCmd :@"-2" ];
        
        CMTime time = self.player.currentTime;
        float f_cur_seconds =  (time.value * 1000 * 0.001  / time.timescale * 1000 * 0.001 );
        float cur_seconds = f_cur_seconds * 1000 * 1000 + 100; //微秒
        long total_time = CMTimeGetSeconds( self.player.currentItem.asset.duration) * 1000 * 1000;
        if(cur_seconds >= total_time ){
            [self.pluginCallBack  sendCmd :@"-3" ];// -3: 当视频播放结束,跳转到答题页
        }
        else{
            [self.pluginCallBack  sendCmd :@"-2" ];// -2: 视频还未播放结束,跳转到视频页
        }
    }
}



@end

