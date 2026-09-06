"use client";

import { DownloadIcon, ExternalLinkIcon, LoaderIcon } from "lucide-react";

import { Button } from "@/components/ui/button";
import { useStandaloneArtifactContent } from "@/core/artifacts/hooks";
import { urlOfArtifact } from "@/core/artifacts/utils";
import { useI18n } from "@/core/i18n/hooks";
import { getFileIcon, getFileName } from "@/core/utils/files";

import { formatArtifactBytes, ArtifactFilePreview } from "./artifact-file-preview";

export function ArtifactViewer({ filepath, threadId, isMock = false }: { filepath: string; threadId: string; isMock?: boolean }) {
  const { t } = useI18n();
  const filename = getFileName(filepath);
  const { content, url, truncated, previewBytes, totalBytes, fullContentRequested, loadFullContent, isLoading, error } = useStandaloneArtifactContent({ filepath, threadId, isMock });
  const isLoadingFullContent = fullContentRequested && isLoading;

  return (
    <div className="bg-background flex h-[100dvh] flex-col">
      <header className="border-border bg-background/90 sticky top-0 z-10 flex shrink-0 items-center gap-2 border-b px-3 py-2.5 backdrop-blur-xl sm:gap-3 sm:px-4 sm:py-3">
        <div className="text-muted-foreground shrink-0">{getFileIcon(filepath, "size-4")}</div>
        <div className="min-w-0 flex-1"><div className="truncate text-sm font-medium sm:text-base" title={filepath}>{filename}</div><div className="text-muted-foreground hidden truncate text-xs sm:block">{filepath}</div></div>
        <Button variant="ghost" size="sm" asChild className="min-h-10 min-w-10 px-2 sm:min-w-0 sm:px-3"><a href={urlOfArtifact({ filepath, threadId, isMock })} target="_blank" rel="noopener noreferrer"><ExternalLinkIcon className="size-4" /><span className="hidden sm:inline">{t.artifactPreview.viewSource}</span></a></Button>
        <Button variant="outline" size="sm" asChild className="min-h-10 min-w-10 px-2 sm:min-w-0 sm:px-3"><a href={urlOfArtifact({ filepath, threadId, isMock, download: true })}><DownloadIcon className="size-4" /><span className="hidden sm:inline">{t.common.download}</span></a></Button>
      </header>
      {truncated && <div className="border-border bg-muted/40 flex shrink-0 flex-col gap-2 border-b px-3 py-2 text-xs sm:flex-row sm:items-center sm:justify-between sm:px-4 sm:text-sm"><span className="text-muted-foreground">{t.artifactPreview.limited(formatArtifactBytes(previewBytes) ?? "1 MiB", formatArtifactBytes(totalBytes))}</span><Button size="sm" variant="outline" onClick={loadFullContent}>{t.artifactPreview.loadFullFile}</Button></div>}
      {isLoadingFullContent && <div className="border-border text-muted-foreground flex shrink-0 items-center gap-2 border-b px-4 py-2 text-sm"><LoaderIcon className="size-4 animate-spin" />{t.artifactPreview.loadingFullFile}</div>}
      <main className="mx-auto min-h-0 w-full max-w-5xl flex-1 overflow-hidden px-0 sm:px-2">
        {error ? <p className="text-muted-foreground p-4 text-sm sm:p-6">{t.artifactPreview.previewFailed}</p> : content === undefined ? <div className="text-muted-foreground flex items-center gap-2 p-4 text-sm sm:p-6"><LoaderIcon className="size-4 animate-spin" />{t.common.loading}</div> : <ArtifactFilePreview content={content} language="markdown" scrollKey={filepath} url={url} />}
      </main>
    </div>
  );
}
