class FileUploader {
    /**
     * @param {File} file
     */
    constructor(file) {
        /**
         * @type {File}
         */
        this.file = file;
    }

    tryToUploadFile() {
        if (this.file.size === 0) {
            common.showError("File is empty");
            return;
        }
        const that = this;
        /* upload file if it was not previously uploaded */
        common.doCallbackIfFileExists(
            this.file.name,
            () => common.showError("File already exists"),
            () => FileUploader.uploadFile(that)
        );
    }

    /**
     * @param {FileUploader} that
     */
    static uploadFile(that) {
        const bytesInMB = 1000000;
        const fileSizeMegabytes = that.file.size / bytesInMB;
        const msg = constants.loaderMessages.uploadingFile;
        common.showLoader(msg.msg + that.file.name, msg.width, () => {
            /* send file by 100MB parts because IDEA server does not allow to send large files */
            const partsCount = Math.ceil(fileSizeMegabytes / 100);
            let countFilesSent = 0; // how many parts were received by server
            let success = true; // if all parts were successfully sent
            for (let i = 0; i < partsCount; i++) {
                const request = new XMLHttpRequest();
                request.onload = () => {
                    countFilesSent++;
                    if (request.status !== 200) { // if something went wrong during upload
                        success = false;
                    }
                    if (countFilesSent === partsCount) { // if all parts uploaded
                        that.endFileUpload(success)
                    }
                };
                request.open("POST", serverNames.FILE, true);
                request.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded');
                request.setRequestHeader('File-Name', that.file.name);
                request.setRequestHeader('File-Part', (i + 1) + "/" + partsCount);
                request.send(that.file.slice(i * bytesInMB * 100, Math.min((i + 1) * bytesInMB * 100, that.file.size)));
            }
        });
    }

    endFileUpload(success) {
        common.hideLoader();
        if (!success) {
            common.showError("File was not sent");
            console.error("File was not sent");
            return;
        }
        console.log("File was sent");
        common.doCallbackIfFileExists(this.file.name,
            () => redirectToFile(this.file.name),
            () => {
                common.showError("File format is unsupported");
                console.error("File was not uploaded");
            });
    }
}

/**
 * Opens a page with uploaded file
 * @param name
 */
function redirectToFile(name) {
    window.location.href = `${serverNames.MAIN_NAME}/${constants.pageName}?file=${encodeURIComponent(name)}`
}

/**
 * @param {File} file
 */
function sendToServer(file) {
    new FileUploader(file).tryToUploadFile();
}

function showSupportedFormats($input) {
    const $button = $input.find(".supported-formats-help");
    const $info = $input.find(".supported-formats");
    const $closeButton = $input.find(".close-supported-formats");

    $button.click(() => {
        /* toggle visibility */
        if ($info.hasClass("visible")) {
            $info.removeClass("visible");
        } else {
            $info.addClass("visible");
        }
    });

    $closeButton.click(() => {
        $info.removeClass("visible");
    });
}

function appendInput($fileList) {
    const $input = $(templates.tree.fileInput().content);
    $input.insertBefore(".file-list-actions");
    $fileList.css("height", "calc(100vh - 190px)");
    showSupportedFormats($input);
}

function listenInput() {
    $('#file').on('change', (e) => {
        const reader = new FileReader();
        const theFile = e.target.files[0];
        // noinspection JSValidateTypes
        reader.onload = ((file) => {
            if (file !== undefined) { // file is undefined if user clicked 'cancel'
                common.hideMessage();
                sendToServer(file);
            }
        })(theFile);
    });
}
