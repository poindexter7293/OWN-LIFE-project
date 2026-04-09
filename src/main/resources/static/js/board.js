document.addEventListener("DOMContentLoaded", function () {
    initBoardImageManager("images", "fileName", "imagePreviewList", 3);
    initBoardImageManager("newImages", "editFileName", "editImagePreviewList", 3);
});

function initBoardImageManager(inputId, fileNameId, previewListId, maxCount) {
    const fileInput = document.getElementById(inputId);
    const fileName = document.getElementById(fileNameId);
    const previewList = document.getElementById(previewListId);

    // 해당 페이지에 요소가 없으면 그냥 종료
    if (!fileInput || !fileName || !previewList) {
        return;
    }

    let selectedFiles = [];

    fileInput.addEventListener("change", function () {
        const newFiles = Array.from(fileInput.files || []);

        if (newFiles.length === 0) {
            syncFileInput(fileInput, selectedFiles);
            updateFileName(fileName, selectedFiles);
            renderPreview(previewList, selectedFiles, fileInput, fileName, maxCount);
            return;
        }

        for (const file of newFiles) {
            // 이미지 파일만 허용
            if (!file.type.startsWith("image/")) {
                continue;
            }

            // 같은 파일 중복 방지 (이름 + 크기 기준)
            const exists = selectedFiles.some(
                f => f.name === file.name && f.size === file.size
            );

            if (exists) {
                continue;
            }

            // 최대 개수 제한
            if (selectedFiles.length >= maxCount) {
                alert(`이미지는 최대 ${maxCount}장까지 업로드할 수 있습니다.`);
                break;
            }

            selectedFiles.push(file);
        }

        syncFileInput(fileInput, selectedFiles);
        updateFileName(fileName, selectedFiles);
        renderPreview(previewList, selectedFiles, fileInput, fileName, maxCount);
    });

    updateFileName(fileName, selectedFiles);
    renderPreview(previewList, selectedFiles, fileInput, fileName, maxCount);
}

function syncFileInput(fileInput, selectedFiles) {
    const dataTransfer = new DataTransfer();

    selectedFiles.forEach(file => {
        dataTransfer.items.add(file);
    });

    fileInput.files = dataTransfer.files;
}

function updateFileName(fileNameElement, selectedFiles) {
    if (!selectedFiles || selectedFiles.length === 0) {
        fileNameElement.textContent = "선택된 파일 없음";
        return;
    }

    if (selectedFiles.length === 1) {
        fileNameElement.textContent = selectedFiles[0].name;
        return;
    }

    fileNameElement.textContent =
        selectedFiles[0].name + " 외 " + (selectedFiles.length - 1) + "개";
}

function renderPreview(previewList, selectedFiles, fileInput, fileNameElement, maxCount) {
    previewList.innerHTML = "";

    selectedFiles.forEach((file, index) => {
        const item = document.createElement("div");
        item.className = "image-preview-item";

        const img = document.createElement("img");
        img.className = "image-preview-thumb";
        img.alt = file.name;

        const removeBtn = document.createElement("button");
        removeBtn.type = "button";
        removeBtn.className = "image-preview-remove";
        removeBtn.textContent = "×";

        removeBtn.addEventListener("click", function () {
            selectedFiles.splice(index, 1);
            syncFileInput(fileInput, selectedFiles);
            updateFileName(fileNameElement, selectedFiles);
            renderPreview(previewList, selectedFiles, fileInput, fileNameElement, maxCount);
        });

        const reader = new FileReader();
        reader.onload = function (e) {
            img.src = e.target.result;
        };
        reader.readAsDataURL(file);

        item.appendChild(img);
        item.appendChild(removeBtn);
        previewList.appendChild(item);
    });
}