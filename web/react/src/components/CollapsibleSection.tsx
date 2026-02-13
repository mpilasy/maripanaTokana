import { useState, useRef, useEffect, type ReactNode } from 'react';

interface CollapsibleSectionProps {
	title: string;
	expanded?: boolean;
	children: ReactNode;
	onShare?: (el: HTMLElement) => void;
}

export default function CollapsibleSection({ title, expanded = false, children, onShare }: CollapsibleSectionProps) {
	const [isExpanded, setIsExpanded] = useState(expanded);
	const contentRef = useRef<HTMLDivElement>(null);
	const innerRef = useRef<HTMLDivElement>(null);
	const [contentHeight, setContentHeight] = useState<number | undefined>(undefined);

	useEffect(() => {
		if (isExpanded && innerRef.current) {
			setContentHeight(innerRef.current.scrollHeight);
		}
	}, [isExpanded, children]);

	function handleShare(e: React.MouseEvent) {
		e.stopPropagation();
		if (contentRef.current && onShare) onShare(contentRef.current);
	}

	return (
		<div className="collapsible-section">
			<div className="section-header" onClick={() => setIsExpanded(v => !v)}>
				<span className="section-title">{title}</span>
				{isExpanded && onShare && (
					<button className="section-share-btn" onClick={handleShare} aria-label="Share">
						<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
							<path d="M4 12v8a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-8"/>
							<polyline points="16 6 12 2 8 6"/>
							<line x1="12" y1="2" x2="12" y2="15"/>
						</svg>
					</button>
				)}
				<span className="section-spacer" />
				<span className={`section-chevron${isExpanded ? ' expanded' : ''}`}>&#9660;</span>
			</div>

			<div
				className="section-content-wrapper"
				style={{
					height: isExpanded ? contentHeight : 0,
					overflow: 'hidden',
					transition: 'height 0.3s ease',
				}}
			>
				<div ref={innerRef}>
					<div className="section-content" ref={contentRef}>
						{children}
					</div>
				</div>
			</div>
		</div>
	);
}
