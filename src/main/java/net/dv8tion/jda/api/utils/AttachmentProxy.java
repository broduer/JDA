/*
 * Copyright 2015 Austin Keener, Michael Ritter, Florian Spieß, and the JDA contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.dv8tion.jda.api.utils;

import net.dv8tion.jda.api.entities.Icon;
import net.dv8tion.jda.internal.utils.Checks;
import net.dv8tion.jda.internal.utils.FutureUtil;
import net.dv8tion.jda.internal.utils.IOUtil;
import okhttp3.OkHttpClient;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/**
 * A utility class to retrieve attachments.
 * <br>This supports downloading the images from the normal URL, as well as downloading the image with a specific width and height.
 */
public class AttachmentProxy extends FileProxy
{
    /**
     * Constructs a new {@link AttachmentProxy} for the provided URL.
     *
     * @param  url
     *         The URL to download the attachment from
     *
     * @throws IllegalArgumentException
     *         If the provided URL is null
     */
    public AttachmentProxy(@Nonnull String url)
    {
        super(url);
    }

    @Nonnull
    @Override
    public AttachmentProxy withClient(@Nonnull OkHttpClient customHttpClient)
    {
        return (AttachmentProxy) super.withClient(customHttpClient);
    }

    /**
     * Returns the attachment URL for the specified width and height.
     * <br>The width and height is a best-effort resize from Discord.
     *
     * @param  width
     *         The width of the image
     * @param  height
     *         The height of the image
     *
     * @throws IllegalArgumentException
     *         If any of the follow checks are true
     *         <ul>
     *             <li>The requested width is negative or 0</li>
     *             <li>The requested height is negative or 0</li>
     *         </ul>
     *
     * @return URL of the attachment with the specified width and height
     */
    @Nonnull
    public String getUrl(int width, int height)
    {
        Checks.positive(width, "Image width");
        Checks.positive(height, "Image height");

        return IOUtil.addQuery(getUrl(), "width", width, "height", height);
    }

    /**
     * Retrieves the {@link InputStream} of this attachment at the specified width and height.
     * <br>The attachment, if an image, may be resized at any size, however if the size does not fit the ratio of the image, then it will be cropped as to fit the target size.
     * <br>If the attachment is not an image then the size parameters are ignored and the file is downloaded.
     *
     * @param  width
     *         The width of this image, must be positive
     * @param  height
     *         The height of this image, must be positive
     *
     * @throws IllegalArgumentException
     *         If any of the follow checks are true
     *         <ul>
     *             <li>The requested width is negative or 0</li>
     *             <li>The requested height is negative or 0</li>
     *         </ul>
     *
     * @return {@link CompletableFuture} which holds an {@link InputStream}, the {@link InputStream} must be closed manually.
     */
    @Nonnull
    @CheckReturnValue
    public CompletableFuture<InputStream> download(int width, int height)
    {
        return download(getUrl(width, height));
    }

    /**
     * Downloads the data of this attachment, at the specified width and height, and stores it in a file with the same name as the queried file name (this would be the last segment of the URL).
     * <br>The attachment, if an image, may be resized at any size, however if the size does not fit the ratio of the image, then it will be cropped as to fit the target size.
     * <br>If the attachment is not an image then the size parameters are ignored and the file is downloaded.
     *
     * <p><b>Implementation note:</b>
     *       The file is first downloaded into a temporary file, the file is then moved to its real destination when the download is complete.
     *
     * @param  width
     *         The width of this image, must be positive
     * @param  height
     *         The height of this image, must be positive
     *
     * @throws IllegalArgumentException
     *         If any of the follow checks are true
     *         <ul>
     *             <li>The requested width is negative or 0</li>
     *             <li>The requested height is negative or 0</li>
     *             <li>The URL's scheme is neither http or https</li>
     *         </ul>
     *
     * @return {@link CompletableFuture} which holds a {@link Path} which corresponds to the location the file has been downloaded.
     *
     */
    @Nonnull
    @CheckReturnValue
    public CompletableFuture<Path> downloadToPath(int width, int height)
    {
        return downloadToPath(getUrl(width, height));
    }

    /**
     * Downloads the data of this attachment, at the specified width and height, and stores it in the specified file.
     * <br>The attachment, if an image, may be resized at any size, however if the size does not fit the ratio of the image, then it will be cropped as to fit the target size.
     * <br>If the attachment is not an image then the size parameters are ignored and the file is downloaded.
     *
     * <p><b>Implementation note:</b>
     *       The file is first downloaded into a temporary file, the file is then moved to its real destination when the download is complete.
     *
     * @param  file
     *         The file in which to download the image
     * @param  width
     *         The width of this image, must be positive
     * @param  height
     *         The height of this image, must be positive
     *
     * @throws IllegalArgumentException
     *         If any of the follow checks are true
     *         <ul>
     *             <li>The target file is null</li>
     *             <li>The parent folder of the target file does not exist</li>
     *             <li>The target file exists and is not a {@link Files#isRegularFile(Path, LinkOption...) regular file}</li>
     *             <li>The target file exists and is not {@link Files#isWritable(Path) writable}</li>
     *             <li>The requested width is negative or 0</li>
     *             <li>The requested height is negative or 0</li>
     *         </ul>
     *
     * @return {@link CompletableFuture} which holds a {@link File}, it is the same as the file passed in the parameters.
     */
    @Nonnull
    @CheckReturnValue
    public CompletableFuture<File> downloadToFile(@Nonnull File file, int width, int height)
    {
        Checks.notNull(file, "File");

        final CompletableFuture<Path> downloadToPathFuture = downloadToPath(getUrl(width, height), file.toPath());
        return FutureUtil.thenApplyCancellable(downloadToPathFuture, Path::toFile);
    }

    /**
     * Downloads the data of this attachment, at the specified size, and stores it in the specified file.
     * <br>The attachment, if an image, may be resized at any size, however if the size does not fit the ratio of the image, then it will be cropped as to fit the target size.
     * <br>If the attachment is not an image then the size parameters are ignored and the file is downloaded.
     *
     * <p><b>Implementation note:</b>
     *       The file is first downloaded into a temporary file, the file is then moved to its real destination when the download is complete.
     *       <br>The given path can also target filesystems such as a ZIP filesystem.
     *
     * @param  path
     *         The file in which to download the image
     * @param  width
     *         The width of this image, must be positive
     * @param  height
     *         The height of this image, must be positive
     *
     * @throws IllegalArgumentException
     *         If any of the follow checks are true
     *         <ul>
     *             <li>The target path is null</li>
     *             <li>The parent folder of the target path does not exist</li>
     *             <li>The target path exists and is not a {@link Files#isRegularFile(Path, LinkOption...) regular file}</li>
     *             <li>The target path exists and is not {@link Files#isWritable(Path) writable}</li>
     *             <li>The requested width is negative or 0</li>
     *             <li>The requested height is negative or 0</li>
     *         </ul>
     *
     * @return {@link CompletableFuture} which holds a {@link Path}, it is the same as the path passed in the parameters.
     */
    @Nonnull
    @CheckReturnValue
    public CompletableFuture<Path> downloadToPath(@Nonnull Path path, int width, int height)
    {
        Checks.notNull(path, "Path");

        return downloadToPath(getUrl(width, height), path);
    }

    /**
     * Downloads the data of this attachment, and constructs an {@link Icon} from the data.
     *
     * @return {@link CompletableFuture} which holds an {@link Icon}.
     */
    @Nonnull
    @CheckReturnValue
    public CompletableFuture<Icon> downloadAsIcon()
    {
        return downloadAsIcon(getUrl());
    }

    /**
     * Downloads the data of this attachment, at the specified size, and constructs an {@link Icon} from the data.
     * <br>The attachment, if an image, may be resized at any size, however if the size does not fit the ratio of the image, then it will be cropped as to fit the target size.
     * <br>If the attachment is not an image then the size parameters are ignored and the file is downloaded.
     *
     * @param  width
     *         The width of this image, must be positive
     * @param  height
     *         The height of this image, must be positive
     *
     * @throws IllegalArgumentException
     *         If any of the follow checks are true
     *         <ul>
     *             <li>The requested width is negative or 0</li>
     *             <li>The requested height is negative or 0</li>
     *         </ul>
     *
     * @return {@link CompletableFuture} which holds an {@link Icon}.
     */
    @Nonnull
    @CheckReturnValue
    public CompletableFuture<Icon> downloadAsIcon(int width, int height)
    {
        return downloadAsIcon(getUrl(width, height));
    }

    /**
     * Returns a {@link FileUpload} which supplies a data stream of this attachment,
     * with the given file name and at the specified size.
     * <br>The returned {@link FileUpload} can be reused safely, and does not need to be closed.
     *
     * <p>The attachment, if an image, may be resized at any size, however if the size does not fit the ratio of the image, then it will be cropped as to fit the target size.
     * <br>If the attachment is not an image then the size parameters are ignored and the file is downloaded.
     *
     * @param  name
     *         The name of the to-be-uploaded file
     * @param  width
     *         The width of this image, must be positive
     * @param  height
     *         The height of this image, must be positive
     *
     * @throws IllegalArgumentException
     *         If any of the follow checks are true
     *         <ul>
     *             <li>The file name is null or blank</li>
     *             <li>The requested width is negative or 0</li>
     *             <li>The requested height is negative or 0</li>
     *         </ul>
     *
     * @return {@link FileUpload} from this attachment.
     */
    @Nonnull
    public FileUpload downloadAsFileUpload(@Nonnull String name, int width, int height)
    {
        final String url = getUrl(width, height); // So the checks are also done outside the FileUpload
        return FileUpload.fromStreamSupplier(name, () ->
        {
            // Blocking is fine on the elastic rate limit thread pool [[JDABuilder#setRateLimitElastic]]
            return download(url).join();
        });
    }
}
