const projectName = getParameter("project");
if (projectName === undefined) {
    console.log("project is not defined");
}
const fileName = getParameter("file");
if (fileName === undefined) {
    console.log("file is not defined");
}

$(window).on("load", () => {
    getFilesList(projectName);
    showProjectsList();
    unableHideFilesList();
});

function showFilesList(projectsDropdown, searchForm, arrowRight, arrowLeft, verticalProjectName) {
    $(".file-menu").css("width", 250);
    projectsDropdown.css("transition", "opacity 300ms");
    projectsDropdown.css("opacity", 1);
    projectsDropdown.css("pointer-events", "auto");
    projectsDropdown.css("position", "relative");
    $("main").css("margin-left", "calc((100vw - 250px - 1200px) / 2 + 250px)");
    // projectsDropdown.show();
    searchForm.show();
    $(".file-form").show();
    arrowLeft.show();
    arrowRight.hide();
    verticalProjectName.css("transition", "");
    verticalProjectName.css("opacity", 0);
    $(".file-list").show();
}

function unableHideFilesList() {
    const arrowLeft = $("#arrow-left");
    const arrowRight = $("#arrow-right");
    const projectsDropdown = $(".projects-dropdown");
    const searchForm = $(".search-form");
    const verticalProjectName = $(".vertical-project-name");
    verticalProjectName.html(projectName === "uploaded-files" ? "Uploaded files" : projectName);
    arrowLeft.click(() => { // hide
        projectsDropdown.css("opacity", 0);
        $(".file-menu").css("width", 40);
        $("main").css("margin-left", "calc((100vw - 40px - 1200px) / 2 + 40px)");
        projectsDropdown.css("transition", "opacity 50ms");
        projectsDropdown.css("pointer-events", "none");
        projectsDropdown.css("position", "absolute");
        // projectsDropdown.hide();
        searchForm.hide();
        $(".file-form").hide();
        arrowLeft.hide();
        arrowRight.show();
        verticalProjectName.css("transition", "opacity 300ms");
        verticalProjectName.css("opacity", 1);
        $(".file-list").hide();
    });
    arrowRight.click(() => { // show
        showFilesList(projectsDropdown, searchForm, arrowRight, arrowLeft, verticalProjectName);
    });
    verticalProjectName.click(() => {
        showFilesList(projectsDropdown, searchForm, arrowRight, arrowLeft, verticalProjectName);
    });
}

function getPageName() {
    return /[^\/]*((?=\?)|(?=\.html))/.exec(window.location.href)[0];
}

function showChooseFile() {
    if (projectName === "uploaded-files") {
        showMessage("Choose or upload file");
    } else {
        showMessage("Choose file");
    }
}

/**
 * @param {String} parameterName
 * @return {undefined|string}
 */
function getParameter(parameterName) {
    const parametersString = window.location.href.split("?")[1];
    if (parametersString === undefined) {
        return undefined;
    } else {
        const parameters = parametersString.split("&");
        for (let i = 0; i < parameters.length; i++) {
            if (parameters[i].startsWith(parameterName + "=")) {
                return parameters[i].substring(
                    parameters[i].indexOf("=") + 1,
                    parameters[i].length
                );
            }
        }
    }
    return undefined;
}

function showNoDataFound() {
    showMessage("No call was registered or all methods took <1ms");
}

/**
 * @param {string} message
 */
function showMessage(message) {
    $("main").append(`<p class='message'>${message}</p>`);
}

function appendInput() {
    const input = templates.tree.fileInput().content;
    $(input).insertBefore("#search-file-form");
}

function updateFilesList(filesList) {
    if (filesList.length === 0) {
        $("<p class='no-file-found'>No file was found</p>").appendTo($(".file-menu"));
    } else {
        const list = templates.tree.listOfFiles({
            fileNames: filesList,
            projectName: projectName,
            pageName: getPageName()
        }).content;
        $(list).appendTo($(".file-menu"));
        if (fileName !== undefined) {
            $("#" + fileName.replace(/\./, "\\.")).addClass("current-file");
        }
    }
    if (projectName === "uploaded-files") {
        appendInput();
        listenInput();
    }
}

function appendProject(project) {
    if (project === projectName ||
        (project === "Uploaded files" && projectName === "uploaded-files")) {
        return;
    }
    const link = "/flamegraph-profiler/" +
        getPageName() +
        "?project=" +
        (project === "Uploaded files" ? "uploaded-files" : project);
    const newElem = $(`<a href='${link}'>${project}</a>`);
    if (project === "Uploaded files") {
        newElem.addClass("uploaded-files-drop-down");
    }
    newElem.appendTo($(".projects-dropdown-content"));
}

function showProjectsList() {
    if (projectName === "uploaded-files") {
        $(".project-name").text("Uploaded files");
    } else {
        $(".project-name").text(projectName);
    }
    const request = new XMLHttpRequest();
    request.open("GET", "/flamegraph-profiler/list-projects", true);
    request.responseType = "json";

    request.onload = function () {
        const projects = request.response;
        for (let i = 0; i < projects.length; i++) {
            appendProject(projects[i]);
        }
        appendProject("Uploaded files");
    };
    request.send();
}

function getFilesList(projectName) {
    const request = new XMLHttpRequest();
    request.open("GET", "/flamegraph-profiler/file-list?project=" + projectName, true);
    request.responseType = "json";

    request.onload = function () {
        const fileNames = request.response;
        if (fileNames.length === 0) {
            updateFilesList([])
        } else {
            updateFilesList(fileNames);
        }
    };
    request.send();
}
