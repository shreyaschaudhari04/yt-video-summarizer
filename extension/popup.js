const generateBtn = document.getElementById("generateBtn");

const loading = document.getElementById("loading");

const result = document.getElementById("result");

const summaryTab = document.getElementById("summaryTab");

const originalTab = document.getElementById("originalTab");

const translatedTab = document.getElementById("translatedTab");

const tabs = document.querySelectorAll(".tab");

const copyBtn = document.getElementById("copyBtn");

const exportTxtBtn = document.getElementById("exportTxtBtn");

const exportPdfBtn = document.getElementById("exportPdfBtn");


// RAW MARKDOWN STORAGE
let summaryMarkdown = "";

let transcriptMarkdown = "";

let translatedMarkdown = "";


// ACTIVE RAW CONTENT
let activeTabContent = "";


// TAB SWITCHING
tabs.forEach(tab => {

    tab.addEventListener("click", () => {

        tabs.forEach(t =>
            t.classList.remove("active")
        );

        document.querySelectorAll(".tab-content")
            .forEach(content =>
                content.classList.remove("active")
            );

        tab.classList.add("active");

        const target =
            tab.getAttribute("data-tab");

        const targetElement =
            document.getElementById(target);

        targetElement.classList.add("active");

        // UPDATE ACTIVE RAW MARKDOWN
        if (target === "summaryTab") {

            activeTabContent = summaryMarkdown;

        } else if (target === "originalTab") {

            activeTabContent = transcriptMarkdown;

        } else if (target === "translatedTab") {

            activeTabContent = translatedMarkdown;
        }
    });
});


// COPY BUTTON
copyBtn.addEventListener("click", async () => {

    try {

        await navigator.clipboard.writeText(activeTabContent);

        copyBtn.textContent = "Copied!";

        setTimeout(() => {

            copyBtn.textContent = "Copy";

        }, 1500);

    } catch (error) {

        console.error(error);

        alert("Failed to copy.");
    }
});


// EXPORT TXT
exportTxtBtn.addEventListener("click", () => {

    try {

        const blob = new Blob(
            [activeTabContent],
            {
                type: "text/plain"
            }
        );

        const url =
            URL.createObjectURL(blob);

        const a =
            document.createElement("a");

        a.href = url;

        a.download = "yt-ai-content.txt";

        document.body.appendChild(a);

        a.click();

        document.body.removeChild(a);

        URL.revokeObjectURL(url);

    } catch (error) {

        console.error(error);

        alert("TXT export failed.");
    }
});


// EXPORT BEAUTIFUL PDF
exportPdfBtn.addEventListener("click", async () => {

    try {

        const pdfTemplate =
            document.getElementById("pdfTemplate");

        const pdfContent =
            document.getElementById("pdfContent");

        const pdfMode =
            document.getElementById("pdfMode");

        const pdfLanguage =
            document.getElementById("pdfLanguage");

        const pdfDate =
            document.getElementById("pdfDate");

        // USER VALUES
        const mode =
            document.getElementById("mode").value;

        const language =
            document.getElementById("language").value || "Original";

        // SET TEMPLATE CONTENT
        pdfMode.textContent = mode;

        pdfLanguage.textContent = language;

        pdfDate.textContent =
            new Date().toLocaleString();

        // RENDER MARKDOWN TO HTML
        pdfContent.innerHTML =
            marked.parse(activeTabContent, {
                breaks: true,
                gfm: true
            });

        // TEMPORARILY SHOW
        pdfTemplate.style.opacity = "1";

        pdfTemplate.style.zIndex = "9999";

        // WAIT FOR FULL RENDER
        await new Promise(resolve =>
            setTimeout(resolve, 800)
        );

        // HTML -> CANVAS
        const canvas = await html2canvas(pdfTemplate, {
            scale: 2,
            useCORS: true,
            backgroundColor: "#ffffff"
        });

        // HIDE AGAIN
        pdfTemplate.style.opacity = "0.01";

        pdfTemplate.style.zIndex = "-9999";

        const imgData =
            canvas.toDataURL("image/png");

        const { jsPDF } = window.jspdf;

        const pdf =
            new jsPDF("p", "mm", "a4");

        const pdfWidth =
            pdf.internal.pageSize.getWidth();

        const pdfHeight =
            pdf.internal.pageSize.getHeight();

        const imgWidth = pdfWidth;

        const imgHeight =
            (canvas.height * imgWidth) / canvas.width;

        let heightLeft = imgHeight;

        let position = 0;

        // FIRST PAGE
        pdf.addImage(
            imgData,
            "PNG",
            0,
            position,
            imgWidth,
            imgHeight
        );

        heightLeft -= pdfHeight;

        // MULTI PAGE SUPPORT
        while (heightLeft > 0) {

            position =
                heightLeft - imgHeight;

            pdf.addPage();

            pdf.addImage(
                imgData,
                "PNG",
                0,
                position,
                imgWidth,
                imgHeight
            );

            heightLeft -= pdfHeight;
        }

        // SAVE
        pdf.save("yt-ai-notes.pdf");

    } catch (error) {

        console.error(error);

        alert("Beautiful PDF export failed.");
    }
});


// GENERATE BUTTON
generateBtn.addEventListener("click", async () => {

    loading.classList.remove("hidden");

    result.classList.add("hidden");

    generateBtn.disabled = true;

    generateBtn.textContent = "Generating...";

    try {

        // CURRENT TAB
        const [tab] = await chrome.tabs.query({
            active: true,
            currentWindow: true
        });

        const videoUrl = tab.url;

        // USER OPTIONS
        const mode =
            document.getElementById("mode").value;

        const language =
            document.getElementById("language").value;

        // API CALL
        const response = await fetch(
            "http://localhost:8080/api/video/summarize",
            {
                method: "POST",

                headers: {
                    "Content-Type": "application/json"
                },

                body: JSON.stringify({
                    url: videoUrl,
                    outputLanguage: language,
                    mode: mode
                })
            }
        );

        const data = await response.json();

        console.log(data);

        // HANDLE ERRORS
        if (!response.ok) {

            alert(data.error || "Something went wrong.");

            loading.classList.add("hidden");

            generateBtn.disabled = false;

            generateBtn.textContent = "Generate";

            return;
        }

        // STORE RAW MARKDOWN
        summaryMarkdown =
            data.summary || "No summary available.";

        transcriptMarkdown =
            data.originalTranscript || "No transcript available.";

        translatedMarkdown =
            data.translatedTranscript || "No translation available.";

        // RENDER MARKDOWN
        summaryTab.innerHTML =
            marked.parse(summaryMarkdown, {
                breaks: true,
                gfm: true
            });

        originalTab.innerHTML =
            marked.parse(transcriptMarkdown, {
                breaks: true,
                gfm: true
            });

        translatedTab.innerHTML =
            marked.parse(translatedMarkdown, {
                breaks: true,
                gfm: true
            });

        // DEFAULT ACTIVE TAB CONTENT
        activeTabContent = summaryMarkdown;

        result.classList.remove("hidden");

    } catch (error) {

        console.error(error);

        alert("Backend connection failed.");
    }

    loading.classList.add("hidden");

    generateBtn.disabled = false;

    generateBtn.textContent = "Generate";
});