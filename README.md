# Logo-Adder
### Simple Android App that adds watermark to videos and photos
App uses FFmpeg kit to add animated watermark to the videos

![Screenshot 2023-02-03 114858](https://user-images.githubusercontent.com/85433014/216597382-800ca0e6-ea86-4377-bf3b-357e7305d04a.png)
![Screenshot_20230203-132232_Pixel Launcher](https://user-images.githubusercontent.com/85433014/216595930-a86e5575-c224-4723-95ea-31906cb6748a.png) <br>
⚠️For Android 11+ : <br>
  User need to give the app the permission to manage all files in case of adding watermark to a video <br>
  (As FFmpeg lib. can't access the watermark image without this permission) <br>

## Features
- Add a watermark to any image in the local storage and preview it before saving, and after taping "Save photo" image saved instantly in pictures folder
<img src="https://user-images.githubusercontent.com/85433014/216597807-ac3f9af7-50e3-4d0f-816f-57c35d1a4341.png" width="250"/>
- User can add a custom watermark and will be previewed (Watermark must have a transparent background for a good experience)
<img src="https://user-images.githubusercontent.com/85433014/216600100-0abc58ce-8ae1-46f4-8440-64ec6e2dc443.png" width="250"/>
- Adding a watermark to videos is also possible and while encoding the output video a loading circle appear to user till video is saved in a folder named "LogoAdder"
<img src="https://user-images.githubusercontent.com/85433014/216604110-0bd564ec-1fb3-4539-aad1-52337c0ac1fc.png" width="250"/>
Took me a 40 sec to add a watermark to a  30sec , 1080p-Video (May vary with device)
