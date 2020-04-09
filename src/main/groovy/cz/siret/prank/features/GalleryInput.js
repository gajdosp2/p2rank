import React, { Component } from 'react'

import ImageInput from './ImageInput';
import MediaInput from './MediaInput';

class GalleryInput extends Component {


  handleRemoveImage = (remove) => {
    console.log(remove);
    /*this.setState(state => {
      let value = this.state.value;
      let index = value.indexOf(remove);
      if (index != -1) {
        value.splice(index, 1);
      }

      return {
        value: value
      }
    });
    */
  }
  handleAddImages = (selected) => {
    console.log(selected);
    /*this.setState(state => {
      let value = this.state.value;

      selected.map(item => {
        if (!value.includes(item)) {
          return value.push(item);
        }
      });

      return {
        value: value
      }
    });
    */
  }

  renderGallery() {
    let returnValue = [];
    //return;
    if (this.props.singleValue) {
      returnValue.push(
        <ImageInput
          key={"gallery_input"}
          handleReturnValue={this.handleReturnValue}
          handleRemoveImage={this.handleRemoveImage}
          value={this.props.value}
        />
      );

    } else {
      this.props.value.map((item, index) => {
        console.log(item);
        return returnValue.push(
          <ImageInput
            key={"gallery_input_" + index}
            handleReturnValue={this.handleReturnValue}
            handleRemoveImage={this.handleRemoveImage}
            value={item}
          />
        );
      });

    }

    return returnValue;

  }

  renderMedia() {

    return (
      <MediaInput
        handleAddImages={this.handleAddImages}
      />
    );

  }

  render() {
    return (
      <div>
        <div>
          <label>{this.props.placeholder}</label>
        </div>
        <div>
          {this.renderGallery() }
        </div>

        <div className="wrapper">
        {/* this.renderMedia() */}
        </div>

      </div>
    );
  }
}

export default GalleryInput;
