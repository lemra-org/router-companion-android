(function () {
    jQuery.fn.fullHeight = function (options) {
        options = $.extend({
            min: 0,
            offset: -200
        }, options);

        this.each(function () {
            var $this = $(this);

            function update () {
                var height = window.innerHeight;
                $this.height(Math.max(height + options.offset, options.min));
            }

            update();

            $(window).on('resize', function () {
                update();
            });
        });
    };
}());

$.fn.setPeek = function (offset){
    // return this.css({
    //     '-webkit-transform': 'translate(0, ' + -offset + 'px)'
    // });
    return this.css({
        top: (offset / 350.0) * 100 + '%'
    });
};

function is_touch_device() {
  return 'ontouchstart' in window // works on most browsers 
      || 'onmsgesturechange' in window; // works on ie10
};

$(function () {
    // $('.hero').fullHeight();

    var images = $('.screens-bar-graph .image'),
        numImages = images.length,
        imageHeight = 350.0,
        minPeek = 40.0,
        maxPeek = imageHeight * 0.8,
        // hoverMode = false,
        inHandler, outHandler;

    images.each(function () {
        var $this = $(this),
            img = $this.find('img');

        $this.attr({
            href: img.attr('src'),
            target: '_blank'
        });
    });

    baguetteBox.run('.screens-bar-graph > .content');

    function updateImages(offset) {
        offset = offset || 0;
        images.each(function (i, el) {
            var t = i / (numImages - 1),
                amplitude = Math.sin(t * Math.PI * 2.0 * 0.8 + offset) * 0.5 + 0.5,
                pos;

            // if (hoverMode) amplitude *= 0.4;

            pos = amplitude * (maxPeek - minPeek) + minPeek;

            // amplitude = amplitude * 0.9 + 0.1;

            // $(el).css({top: ((1.0 - amplitude) * 100) + '%'});
            $(el).setPeek(pos);
        });
    }

    function updateImagesFromScroll() {
        var scrollTop = $(window).scrollTop();
        // updateImages(scrollTop * -0.003 - 0.8);
        updateImages(scrollTop * -0.001 - 3.11);
    }

    // images.click(function(e) {
    //     e.preventDefault();
    //     var overlay = $('<div class="overlay"><img src="</div>')
    // });

    if (is_touch_device()) {
        $('html').addClass('touch');
    } else {
        $(window).scroll(function (e) {
            // if (hoverMode) return;
            updateImagesFromScroll();
        });

        // $('.screens-bar-graph > .content').hover(function (e) {
        //     // hoverMode = true;
        //     $('.screens-bar-graph').addClass('active');
        // }, function (e) {
        //     // hoverMode = false;
        //     $('.screens-bar-graph').removeClass('active');
        //     // updateImagesFromScroll();
        // });

        // images.hover(function (e) {
        //     clearTimeout(inHandler);
        //     clearTimeout(outHandler);

        //     var el = this;

        //     function doIt() {
        //         hoverMode = true;
        //         updateImages();
        //         $(el).setPeek(imageHeight);
        //     }

        //     if (hoverMode) {
        //         doIt();
        //     } else {
        //         inHandler = setTimeout(doIt, 300);
        //     }        
        // }, function (el) {
        //     clearTimeout(inHandler);
        //     clearTimeout(outHandler);
        //     updateImages();

        //     outHandler = setTimeout(function () {
        //         hoverMode = false;
        //         updateImages();
        //     }, 200);
        // });
    }

    updateImages(-79.6);
});