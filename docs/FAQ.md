# Trouble Shooting & FAQ

## Can not find Phonograph Plus in Android Auto

Well, it is because Phonograph Plus is an _Unknown Source_ application (Well, looks like that _only_ applications installed by Google Play can be
Known Source). _Unknown Source_ applications **won't be displayed** in  Android Auto's launcher settings.

So you need trust "_Unknown Source_ applications":

- Click Android Auto's `version` multiple times to enter `Developer Mode``.
- Go to Developer Settings in right-top menu
- Click the second last one: `Unknown Source`
- Check Launcher Settings again

Actually, besides using third-party installers which faking the source to Google Play, there is no way to fix such problems.
(Or you can distribute your app to Google Play, but Phonograph Plus has never such plan).

By the way, it seems that Developer Mode can be turned off once Unknown Sources are allowed
(This doesn't turn off along with Developer Mode).

_See Also Issue: #109_

## Random Album Artwork

_"The artwork displays fine in the tiny folder view icons but when playing the song the artwork defaults to a seemingly random artwork in that
folder and displays it for each song in the folder."_

This happened when you put untagged audio files (whose tags have empty "album" field) in one directory. Because we read album cover from
Android `MediaStore` (if you enabled in Image Source), and Android take the folder name (actually containing path but not showing) as these
files' Album name. So these all audio files are considered as an album, which means any cover artwork of these files are considered as "same"
leading random artwork.

**How to fix it:**

- Do not put them into one folder;
- Add distinct Album tags to these files in one folder.

_See Also Issue: #6_

## WMA support?

**No**.

`Windows Media Audio (WMA)` is not supported by Android OS. 
And Phonograph Plus is currently relaying on OS to decode media.


**Workaround**

- Convert your audio files to other format. (**Suggested**)

   `WMA` is Microsoft's format/codecs, is proprietary and non-free. Due to license conflicting or concerning of patents, many open source
   software (like AOSP) won't support it. So, try to convert these audio to other open and free formats, which are "more compatible" for most
   platforms.


- Or wait for me to implement integrating a 3rd-party decoder (like `ffmpeg`) to Phonograph Plus.

   But this is temporarily planning on late 2024 or early 2025.

_See Also Issue: #112_
