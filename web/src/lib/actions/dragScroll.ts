/**
 * Svelte action: lets a mouse drag a horizontally scrolling row, as a finger can. Touch and pen keep
 * their native scrolling. A drag of more than a few pixels swallows the click that ends it, so
 * dragging across a tab or card doesn't also select it.
 */
export function dragScroll(node: HTMLElement) {
	let startX = 0;
	let startScroll = 0;
	let pointerId: number | null = null;
	let dragged = false;

	const down = (e: PointerEvent) => {
		if (e.pointerType !== 'mouse' || e.button !== 0 || node.scrollWidth <= node.clientWidth) return;
		pointerId = e.pointerId;
		startX = e.clientX;
		startScroll = node.scrollLeft;
		dragged = false;
	};
	const move = (e: PointerEvent) => {
		if (e.pointerId !== pointerId) return;
		const dx = e.clientX - startX;
		if (!dragged && Math.abs(dx) < 5) return;
		if (!dragged) {
			dragged = true;
			node.setPointerCapture(e.pointerId);
			node.style.cursor = 'grabbing';
		}
		node.scrollLeft = startScroll - dx;
	};
	const up = (e: PointerEvent) => {
		if (e.pointerId !== pointerId) return;
		pointerId = null;
		node.style.cursor = '';
		if (node.hasPointerCapture(e.pointerId)) node.releasePointerCapture(e.pointerId);
	};
	const swallowClick = (e: MouseEvent) => {
		if (dragged) {
			e.preventDefault();
			e.stopImmediatePropagation();
			dragged = false;
		}
	};
	// Stops the browser dragging an image or selecting text instead of scrolling
	const noNativeDrag = (e: Event) => e.preventDefault();

	node.addEventListener('pointerdown', down);
	node.addEventListener('pointermove', move);
	node.addEventListener('pointerup', up);
	node.addEventListener('pointercancel', up);
	node.addEventListener('click', swallowClick, true);
	node.addEventListener('dragstart', noNativeDrag);
	return {
		destroy() {
			node.removeEventListener('pointerdown', down);
			node.removeEventListener('pointermove', move);
			node.removeEventListener('pointerup', up);
			node.removeEventListener('pointercancel', up);
			node.removeEventListener('click', swallowClick, true);
			node.removeEventListener('dragstart', noNativeDrag);
		},
	};
}
