# react-native-vlcplayer
A `<VLCPlayer>` component for react-native
project clone from `https://github.com/Nghi-NV/react-native-vlcplayer.git`


### Add it to your project

Run `npm install --save react-native-vlcplayer2`

or

Run `yarn add react-native-vlcplayer2`

or

Run `yarn add https://github.com/rich1111/react-native-vlcplayer.git`


## android/ios link

Run `react-native link react-native-vlcplayer2`


## ios

Use framework
1. cd to ios
2. run `pod install`

Enable Bitcode
in root project select Build Settings ---> find Bitcode and select Disable Bitcode


## Use
````
  (1) import { VLCPlayer, VlCPlayerView } from 'react-native-vlcplayer';

  (2)
    <VLCPlayer
           ref={ref => (this.vlcPlayer = ref)}
           style={[styles.video]}
           videoAspectRatio="16:9"
           paused={this.state.paused}
           source={{ uri: this.props.uri}}
           onProgress={this.onProgress.bind(this)}
           onEnd={this.onEnded.bind(this)}
           onBuffering={this.onBuffering.bind(this)}
           onError={this._onError}
           onStopped={this.onStopped.bind(this)}   
           onPlaying={this.onPlaying.bind(this)}
           onPaused={this.onPaused.bind(this)}      
       />
  (3) or use
    <VlCPlayerView
           autoplay={false}
           url={this.state.url}
           Orientation={Orientation}      
           //BackHandle={BackHandle}
           ggUrl=""
           showGG={true}
           showTitle={true}
           title=""
           showBack={true}
           onLeftPress={()=>{}}
           startFullScreen={() => {      
              this.setState({
              isFull: true,
             });
           }}
           closeFullScreen={() => {
              this.setState({
              isFull: false,
             });
           }}
       />
````
![1574120354357.jpg](./images/1574120354357.jpg)

![1574120350599.jpg](./images/1574120350599.jpg)
